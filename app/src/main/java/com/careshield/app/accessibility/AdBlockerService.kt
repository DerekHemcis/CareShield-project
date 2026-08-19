package com.careshield.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.careshield.app.data.SettingsRepo

/**
 * 亲安盾核心：无障碍广告拦截服务（增强版）
 * 多策略识别：文字关键词 / 倒计时 / contentDescription / 角落关闭按钮 / 信息流"广告"标签滑动
 */
class AdBlockerService : AccessibilityService() {

    @Volatile private var cooldown = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - cooldown < 100) return
        cooldown = now

        // 使用缓存的开关状态（避免每次事件 runBlocking 读 DataStore 造成掉帧）
        // 每 2 秒刷新一次缓存
        if (now - cacheTime > 2000) {
            refreshSwitchCache()
        }
        if (!cachedSkipAd && !cachedClosePop && !cachedSwipeAd && !cachedAggressive) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        handleAd(root, cachedSkipAd, cachedClosePop, cachedSwipeAd)
    }

    @Volatile private var cachedSkipAd = false
    @Volatile private var cachedClosePop = false
    @Volatile private var cachedSwipeAd = false
    @Volatile private var cachedAggressive = false
    @Volatile private var cacheTime = 0L

    private fun refreshSwitchCache() {
        val repo = SettingsRepo(this)
        cachedSkipAd = repo.isEnabledSync("func_skip_ad")
        cachedClosePop = repo.isEnabledSync("func_close_pop")
        cachedSwipeAd = repo.isEnabledSync("func_misclick")
        cachedAggressive = repo.isEnabledSync("func_aggressive_skip")
    }

    private fun handleAd(root: AccessibilityNodeInfo, skipAd: Boolean, closePop: Boolean, swipeAd: Boolean) {
        val pkg = packageName ?: return
        val repo = SettingsRepo(this)

        // 单机/离线/工具类应用基本无广告：跳过拦截，避免误杀（如计算器、相册、日历、输入法）
        if (isNoAdApp(pkg)) return
        // 微信本体：广告较少，仅保留"跳过"级拦截，不做激进弹窗/箭头处理
        val isWeChat = pkg == "com.tencent.mm"

        val whitelistMode = repo.isEnabledSync("whitelist_mode")
        val blockWeb = repo.isEnabledSync("func_block_web")

        // 白名单保护模式：开启时，仅"网页拦截"仍扫；否则白名单应用直接豁免
        val isWhitelistedApp = repo.isWhitelisted(pkg)
        if (!whitelistMode && isWhitelistedApp) {
            return  // 非白名单模式：白名单内应用（微信/抖音等）不拦截
        }
        // 白名单模式开启时，对白名单应用内部的信息流广告仍可拦截（func_block_web 生效时）
        if (whitelistMode && isWhitelistedApp && !blockWeb) {
            return
        }

        // 判断当前界面是否存在"广告上下文"（界面上有 广告/Ad/Sponsored 标签）
        val hasAdContext = if (skipAd || closePop) detectAdContext(root) else false

        // ---- 策略1: 智能置信度评分，点击广告按钮 ----
        if (skipAd || closePop) {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            collectAll(root, nodes)
            for (node in nodes) {
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val combined = "$text $desc"
                if (combined.isBlank()) continue
                if (node.className?.toString()?.contains("EditText") == true) continue

                // 关键：排除"返回/箭头/上一页"类节点，防误点返回导致界面回退
                if (BACK_ANY.any { text.contains(it) } || BACK_ANY.any { desc.contains(it, ignoreCase = true) }) {
                    continue
                }

                val score = scoreNode(combined, text, node, hasAdContext, skipAd, closePop)
                if (score >= SCORE_CLICK) {
                    val target = findClickableAncestor(node) ?: node
                    val source = if (score >= SCORE_STRONG) "广告" else if (skipAd) "广告" else "弹窗"
                    clickIfSafe(target, repo, source)
                    return
                }
            }
        }

        // ---- 激进跳过（可选项 func_aggressive_skip）：全屏广告 + 角落倒计时 ----
        if (cachedAggressive) {
            if (aggressiveSkipTry(root, repo)) return
        }
        // ---- 策略2: 信息流"广告"标签 → 上滑跳过 ----
        if (swipeAd && AD_LABEL.any { hasTextNode(root, it) }) {
            performSwipeUp()
        }

        // ---- 策略3: 防误触下载 ----
        val blockDownload = repo.isEnabledSync("func_misclick")
        if (blockDownload) {
            blockMisclickDownload(root)
        }
    }

    /**
     * 防误触下载：检测到"下载/安装/应用商店/立即下载"等误触下载界面时，自动返回退回。
     * 识别到下载/安装语义的按钮或页面，执行返回键，防止老人误触跳转下载。
     */
    /**
     * 激进跳过：全屏广告启发式。
     * 检测：屏幕大部分区域被无文字的大容器(图片/视频广告)占据 + 角落有倒计时数字/小按钮
     * → 尝试点击右下角/右上角跳过按钮（开屏广告常见位置）。
     * 仅在 func_aggressive_skip 开启时调用。可能误触，故设为独立可选项。
     */
    private fun aggressiveSkipTry(root: AccessibilityNodeInfo, repo: SettingsRepo): Boolean {
        // 只对视频/内容类App的高频开屏广告生效，工具/设置类不激进
        if (isNoAdApp(packageName ?: "")) return false
        // 防抖：避免持续点
        if (System.currentTimeMillis() - lastAggressive < 3000) return false

        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root, nodes)
        val screen = android.graphics.Rect()
        try { rootInActiveWindow?.getBoundsInScreen(screen) } catch (_: Exception) { return false }
        if (screen.width() <= 0 || screen.height() <= 0) return false

        // 1) 找角落倒计时数字按钮（纯数字或 数字+跳过, 位于屏幕顶部两侧）
        for (node in nodes) {
            val t = node.text?.toString() ?: ""
            if (t.isBlank()) continue
            // 匹配倒计时数字：s/秒/纯数字 且位于屏幕顶部 1/3、两侧 1/3
            val isCountdown = REGEX_COUNTDOWN.matches(t.trim()) ||
                (t.length <= 5 && t.all { it.isDigit() })
            if (!isCountdown) continue
            val b = android.graphics.Rect()
            try { node.getBoundsInScreen(b) } catch (_: Exception) { continue }
            if (b.top < screen.height() * 0.4 && (b.left < screen.width() * 0.3 || b.right > screen.width() * 0.7)) {
                val target = findClickableAncestor(node) ?: node
                lastAggressive = System.currentTimeMillis()
                clickIfSafe(target, repo, "广告(激进)")
                return true
            }
        }
        return false
    }

    @Volatile private var lastAggressive = 0L
    private val REGEX_COUNTDOWN = Regex("\\d+\\s*(s|秒|后)?|跳过.*\\d|\\d.*跳过")

    private fun blockMisclickDownload(root: AccessibilityNodeInfo) {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root, nodes)
        // 检测页面上是否有下载/安装/商店特征文本
        var hitDownload = false
        for (node in nodes) {
            val text = (node.text?.toString() ?: "") + (node.contentDescription?.toString() ?: "")
            if (text.isBlank()) continue
            // 下载/安装/商店特征（避免"下载中""已完成"这类状态词触发）
            if (DOWNLOAD_ANY.any { text.contains(it) } && !STATUS_ANY.any { text.contains(it) }) {
                hitDownload = true
            }
        }
        if (hitDownload) {
            // 节流防误返回
            if (System.currentTimeMillis() - lastMisclick > 1200) {
                lastMisclick = System.currentTimeMillis()
                performGlobalAction(GLOBAL_ACTION_BACK)
                // 记录
                Thread { kotlinx.coroutines.runBlocking { SettingsRepo(this@AdBlockerService).logIntercept("防误触", "拦截下载页面") } }.start()
            }
        }
    }

    /**
     * 置信度评分：返回分数越高越可能是真广告按钮。
     * 强证据(>=SCORE_STRONG)：明确广告语义词
     * 中证据(>=SCORE_MID)：广告上下文下的关闭/跳过
     * 弱证据：仅 x/取消 之类，必须同时有广告上下文 + 在角落，否则不打分
     */
    private fun scoreNode(combined: String, text: String, node: AccessibilityNodeInfo,
                          adContext: Boolean, skipAd: Boolean, closePop: Boolean): Int {
        var score = 0
        // 强：明确"跳过广告/关闭广告/点击跳过"
        if (skipAd && STRONG_SKIP.any { combined.contains(it) }) score = maxOf(score, SCORE_STRONG)
        if (closePop && STRONG_CLOSE.any { combined.contains(it) }) score = maxOf(score, SCORE_STRONG)

        if (score >= SCORE_STRONG) return score

        // 高置信：含"跳过/Skip"（开屏广告常见，含倒计时如 1s跳过 3跳过 跳过 10；不依赖广告上下文）
        // 不要求节点自身 clickable：匹配后统一找可点击祖先；与倒计时数字绑定，避免误杀日历/时间
        if (skipAd && (combined.contains("跳过") || combined.contains("Skip"))) {
            score = maxOf(score, SCORE_MID)
        }
        // 倒计时+跳过绑定：数字与"跳过"在同一按钮（如 "跳过 10" "3s跳过" "跳过广告 5 秒"）
        if (skipAd && COUNTDOWN_SKIPregex.matches(text.trim())) {
            score = maxOf(score, SCORE_MID)
        }

        // 中：广告上下文存在时的通用跳过/关闭
        if (skipAd && adContext && MID_SKIP.any { combined.contains(it) }) score = maxOf(score, SCORE_MID)
        if (closePop && adContext && MID_CLOSE.any { combined.contains(it) }) score = maxOf(score, SCORE_MID)
        if (score >= SCORE_MID) return score

        // 弱：裸 x/×/知道了（无明确广告语义）
        // 必须：有广告上下文 + 节点在屏幕角落 + 非页面级关闭
        if (closePop && adContext && WEAK_CLOSE.any { combined.trim() == it || combined.contains(it) }) {
            if (node.isClickable && isCornerNode(node)) {
                score = maxOf(score, SCORE_WEAK)
            }
        }
        return score
    }

    /** 检测界面是否有"广告/Ad/Sponsored"标签（作为广告上下文） */
    private fun detectAdContext(root: AccessibilityNodeInfo): Boolean {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root, nodes)
        return nodes.any { node ->
            val t = node.text?.toString() ?: ""
            node.isClickable == false && (t == "广告" || t == "推广" || t == "Ad" || t == "广告" ||
                AD_LABEL.any { t.trim() == it })
        }
    }

    /** 节点是否在屏幕角落（右上/左上小区域，通常是广告关闭按钮） */
    private fun isCornerNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val r = android.graphics.Rect()
            node.getBoundsInScreen(r)
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            // 顶部 22% 内
            if (r.top >= screenH * 0.22f) return false
            // 靠近右边缘（右 25%）或左边缘（左 25%）
            val nearRight = r.left > screenW * 0.75f
            val nearLeft = r.right < screenW * 0.25f
            // 且按钮本身不大（小关闭按钮）
            val small = r.width() <= screenW * 0.3f && r.height() <= screenH * 0.12f
            (nearRight || nearLeft) && small
        } catch (_: Exception) { false }
    }

    private fun hasTextNode(root: AccessibilityNodeInfo, keyword: String): Boolean {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root, nodes)
        return nodes.any {
            val t = it.text?.toString() ?: ""
            (t == keyword || t.contains(keyword)) && it.isClickable == false && t.length < 8
        }
    }

    /** 单机/离线/工具类应用（无广告，跳过拦截避免误杀） */
    private fun isNoAdApp(pkg: String): Boolean {
        return NO_AD_PKGS.any { pkg.startsWith(it) }
    }

    // 单机/离线/工具类判断引用 NO_AD_PKGS（见文件末尾 companion）
    private fun performSwipeUp() {
        if (System.currentTimeMillis() - lastSwipeAd < 1500) return
        lastSwipeAd = System.currentTimeMillis()
        val bounds = rootInActiveWindow?.let {
            try {
                val r = android.graphics.Rect(); it.getBoundsInScreen(r); r
            } catch (_: Exception) { null }
        } ?: return
        val sx = (bounds.left + bounds.width() / 2).toFloat()
        val sy1 = (bounds.top + bounds.height() * 0.7f)
        val sy2 = (bounds.top + bounds.height() * 0.3f)
        val path = Path().apply { moveTo(sx, sy1); lineTo(sx, sy2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
        android.util.Log.i("CareShield", "信息流广告检测到，上滑跳过 (pkg=$packageName)")
    }

    private fun collectAll(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectAll(it, out) }
        }
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = node
        var i = 0
        while (cur != null && i < 8) {
            if (cur.isClickable) return cur
            cur = cur.parent
            i++
        }
        return null
    }

    private fun clickIfSafe(node: AccessibilityNodeInfo, repo: SettingsRepo, source: String) {
        val pkg = packageName ?: return
        val action = (node.text?.toString() ?: node.contentDescription?.toString() ?: "按钮")
        // 诊断：识别到广告按钮即打印（降频）
        if (System.currentTimeMillis() - lastDiagDump > 2000) {
            lastDiagDump = System.currentTimeMillis()
            android.util.Log.i("CareShield", "命中[$source]: $action pkg=$pkg")
        }
        try {
            var ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // 兜底：若节点点击失败，尝试可点击祖先
            if (!ok) {
                val anc = findClickableAncestor(node)
                if (anc != null && anc !== node) ok = anc.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            if (ok) {
                // 记录时带应用作用域（在哪个 app 里拦截）
                val scopeInfo = "$pkg · ${node.text?.toString() ?: node.contentDescription?.toString() ?: "按钮"}"
                Thread { kotlinx.coroutines.runBlocking { repo.logIntercept(source, scopeInfo) } }.start()
            } else {
                android.util.Log.i("CareShield", "点击失败: $action pkg=$pkg")
            }
        } catch (_: Exception) { }
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    @Volatile private var lastSwipeAd = 0L
    @Volatile private var lastDiagDump = 0L
    @Volatile private var lastMisclick = 0L

    companion object {
        // 强证据：明确广告语义，直接拦截
        private val STRONG_SKIP = listOf("跳过广告", "点击跳过广告", "广告可跳过", "播放后跳过")
        private val STRONG_CLOSE = listOf("关闭广告", "关闭弹窗", "关闭广告悬浮窗")

        // 独立"跳过"：作为可点击按钮时视为开屏广告（不依赖广告上下文）
        private val STANDALONE_SKIP = listOf("跳过", "跳過", "Skip", "点击跳过")

        // 中证据：有广告上下文(界面有"广告"标签)时才拦
        private val MID_SKIP = listOf("跳过", "Skip", "点击跳过", "跳過")
        private val MID_CLOSE = listOf("关闭", "关掉", "关闭广告", "Close", "Dismiss", "Got it", "Not now", "取消")

        // 弱证据：裸关闭词——已移除裸 x/×（避免误点返回箭头/关闭标签）
        // 仅当 广告上下文 + 角落 + 明确按钮 + 非返回 才考虑（见 scoreNode 逻辑）
        private val WEAK_CLOSE = listOf("知道了")

        // 返回/箭头/上一页 排除词（防误点导致回退）
        private val BACK_ANY = listOf("返回", "上一页", "Back", "back", "‹", "‹‹", "←")

        private val AD_LABEL = listOf("广告", "推广", "Sponsored", "Ad")

        // 无广告/单机/工具类应用前缀（本地工具、系统组件、输入法等，跳过拦截防误杀）
        private val NO_AD_PKGS = listOf(
            "com.android.calculator", "com.android.gallery", "com.android.calendar",
            "com.android.deskclock", "com.coloros.filemanager", "com.coloros.gallery",
            "com.sohu.inputmethod", "com.monet.inputmethod", "com.android.settings",
            "com.android.systemui", "com.android.launcher",
            "com.coloros.note", "com.android.documentsui", "com.android.contacts"
        )

        // 倒计时+跳过 绑定正则：数字与"跳过"同节点（跳过在前/在后），避免裸数字误杀
        private val COUNTDOWN_SKIPregex = Regex(
            "跳过.{0,4}[0-9]+|[0-9]+.{0,4}跳过"
        )

        // 防误触下载：下载/安装/商店特征词
        private val DOWNLOAD_ANY = listOf(
            "立即下载", "马上安装", "去下载", "下载安装", "安装应用", "前往应用商店",
            "打开应用商店", "查看详情去下载", "免费下载", "抢先下载", "极速下载"
        )
        // 状态词（不应触发返回）
        private val STATUS_ANY = listOf("下载中", "下载完成", "已下载", "安装完成", "0%", "100%", "%")

        // 置信度阈值
        private const val SCORE_WEAK = 40
        private const val SCORE_MID = 70
        private const val SCORE_STRONG = 100
        private const val SCORE_CLICK = 40 // 达到此分数即可点击
    }
}
