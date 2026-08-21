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
 * v1.x 增强：ACTION_CLICK 失败时用真实坐标手势点击兜底（解决腾讯视频/搜狐视频等自定义View广告跳过无效）
 */
class AdBlockerService : AccessibilityService() {

    @Volatile private var cooldown = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - cooldown < 100) return
        cooldown = now

        // 使用缓存的开关状态（避免每次事件 runBlocking 读 DataStore 造成掉帧）
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
        // 关键：绝不拦截/干扰 CareShield 自己（避免自识别自点击）
        if (pkg == "com.careshield.app") return
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
                    // 视频类 App（腾讯/搜狐等）：直接传"跳过"文字节点本身给坐标点击，
                    // 避免用可点击祖先整块区域（点中心可能误进广告）。其余 App 用可点击祖先。
                    val target = if (GESTURE_FIRST_PKGS.any { pkg.startsWith(it) }) node
                                 else (findClickableAncestor(node) ?: node)
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
     * 激进跳过：全屏广告启发式。
     * 检测：屏幕大部分区域被无文字的大容器(图片/视频广告)占据 + 角落有倒计时数字/小按钮
     * → 尝试点击右下角/右上角跳过按钮（开屏广告常见位置）。
     * 仅在 func_aggressive_skip 开启时调用。可能误触，故设为独立可选项。
     */
    private fun aggressiveSkipTry(root: AccessibilityNodeInfo, repo: SettingsRepo): Boolean {
        if (isNoAdApp(packageName ?: "")) return false
        if (System.currentTimeMillis() - lastAggressive < 3000) return false

        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAll(root, nodes)
        val screen = android.graphics.Rect()
        try { rootInActiveWindow?.getBoundsInScreen(screen) } catch (_: Exception) { return false }
        if (screen.width() <= 0 || screen.height() <= 0) return false

        for (node in nodes) {
            val t = node.text?.toString() ?: ""
            if (t.isBlank()) continue
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
        var hitDownload = false
        for (node in nodes) {
            val text = (node.text?.toString() ?: "") + (node.contentDescription?.toString() ?: "")
            if (text.isBlank()) continue
            if (DOWNLOAD_ANY.any { text.contains(it) } && !STATUS_ANY.any { text.contains(it) }) {
                hitDownload = true
            }
        }
        if (hitDownload) {
            if (System.currentTimeMillis() - lastMisclick > 1200) {
                lastMisclick = System.currentTimeMillis()
                performGlobalAction(GLOBAL_ACTION_BACK)
                Thread { kotlinx.coroutines.runBlocking { SettingsRepo(this@AdBlockerService).logIntercept("防误触", "拦截下载页面") } }.start()
            }
        }
    }

    /**
     * 置信度评分：返回分数越高越可能是真广告按钮。
     */
    private fun scoreNode(combined: String, text: String, node: AccessibilityNodeInfo,
                          adContext: Boolean, skipAd: Boolean, closePop: Boolean): Int {
        var score = 0
        // 强：明确"跳过广告/关闭广告/点击跳过"
        if (skipAd && STRONG_SKIP.any { combined.contains(it) }) score = maxOf(score, SCORE_STRONG)
        if (closePop && STRONG_CLOSE.any { combined.contains(it) }) score = maxOf(score, SCORE_STRONG)

        if (score >= SCORE_STRONG) return score

        // 高置信：含"跳过/Skip"
        if (skipAd && (combined.contains("跳过") || combined.contains("Skip"))) {
            score = maxOf(score, SCORE_MID)
        }
        // 倒计时+跳过绑定
        if (skipAd && COUNTDOWN_SKIPregex.matches(text.trim())) {
            score = maxOf(score, SCORE_MID)
        }

        // 中：广告上下文存在时的通用跳过/关闭
        if (skipAd && adContext && MID_SKIP.any { combined.contains(it) }) score = maxOf(score, SCORE_MID)
        if (closePop && adContext && MID_CLOSE.any { combined.contains(it) }) score = maxOf(score, SCORE_MID)
        if (score >= SCORE_MID) return score

        // 弱：裸 x/×/知道了（必须广告上下文 + 角落 + 可点击）
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
            node.isClickable == false && AD_LABEL.any { t.trim() == it }
        }
    }

    /** 节点是否在屏幕角落（右上/左上小区域，通常是广告关闭按钮） */
    private fun isCornerNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val r = android.graphics.Rect()
            node.getBoundsInScreen(r)
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            if (r.top >= screenH * 0.22f) return false
            val nearRight = r.left > screenW * 0.75f
            val nearLeft = r.right < screenW * 0.25f
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

    /**
     * 增强点击：ACTION_CLICK 失败 -> 尝试可点击祖先 -> 再用真实坐标手势点击兜底。
     * 解决腾讯视频(com.tencent.qqlive)、搜狐视频(com.sohu.sohuvideo) 等自定义 View/图片跳过按钮
     * 无法用 performAction(ACTION_CLICK) 触发的问题（搜狐日志有"点击了未见效"，实为点击未落到按钮）。
     */
    private fun clickIfSafe(node: AccessibilityNodeInfo, repo: SettingsRepo, source: String) {
        val pkg = packageName ?: return
        val action = (node.text?.toString() ?: node.contentDescription?.toString() ?: "按钮")
        if (System.currentTimeMillis() - lastDiagDump > 2000) {
            lastDiagDump = System.currentTimeMillis()
            android.util.Log.i("CareShield", "命中[$source]: $action pkg=$pkg")
        }
        try {
            var ok = false
            // 针对已知 performAction(ACTION_CLICK) 无效的视频类 App（腾讯视频/搜狐视频等），
            // 直接优先用真实坐标手势点击。注意：坐标点"跳过"文字节点自身中心，
            // 而非可点击祖先（祖先是整个广告容器时点中心会误进广告）。
            if (GESTURE_FIRST_PKGS.any { pkg.startsWith(it) }) {
                ok = coordinateTapOnNode(node)
            } else {
                // 常规 App：先节点 ACTION_CLICK，失败再用坐标兜底
                ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!ok) {
                    val anc = findClickableAncestor(node)
                    if (anc != null && anc !== node) ok = anc.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                if (!ok) {
                    android.util.Log.i("CareShield", "ACTION_CLICK失败，改用坐标手势点击: $action pkg=$pkg")
                    ok = coordinateTapOnNode(node)
                }
            }

            if (ok) {
                val scopeInfo = "$pkg · ${node.text?.toString() ?: node.contentDescription?.toString() ?: "按钮"}"
                Thread { kotlinx.coroutines.runBlocking { repo.logIntercept(source, scopeInfo) } }.start()
            } else {
                android.util.Log.i("CareShield", "点击失败: $action pkg=$pkg")
            }
        } catch (_: Exception) { }
    }

    /**
     * 用真实触摸手势点击节点中心坐标。对 performAction 无效的自定义 View/SurfaceView 广告有效。
     */
    private fun coordinateTapOnNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val r = android.graphics.Rect()
            node.getBoundsInScreen(r)
            if (r.isEmpty || r.width() <= 0 || r.height() <= 0) return false
            // 按钮中心点（跳过按钮一般不大，直接点中心）
            val cx = (r.left + r.width() / 2).toFloat()
            val cy = (r.top + r.height() / 2).toFloat()
            val path = Path().apply { moveTo(cx, cy); lineTo(cx + 1, cy + 1) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
                .build()
            // dispatchGesture 返回 true 即代表手势成功派发（onCompleted 会异步在主线程回调，
            // 这里不阻塞等待，避免在主线程 await 导致回调无机会执行）。
            val dispatched = dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    // 派发成功（异步回调），无需额外处理
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    android.util.Log.i("CareShield", "坐标点击被取消 pkg=$packageName")
                }
            }, null)
            android.util.Log.i("CareShield", "坐标点击节点中心($cx,$cy) pkg=$packageName dispatched=$dispatched")
            dispatched
        } catch (_: Exception) {
            android.util.Log.i("CareShield", "坐标点击异常: ${packageName}")
            false
        }
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

        // 中证据：有广告上下文(界面有"广告"标签)时才拦
        private val MID_SKIP = listOf("跳过", "Skip", "点击跳过", "跳過")
        private val MID_CLOSE = listOf("关闭", "关掉", "关闭广告", "Close", "Dismiss", "Got it", "Not now", "取消")

        // 弱证据：裸关闭词
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

        // 视频类 App（performAction(ACTION_CLICK) 对这些 App 的开屏广告按钮常无效，
        // 必须用真实坐标手势点击才能跳过，否则出现"点了跳过但广告未消失"）。
        private val GESTURE_FIRST_PKGS = listOf(
            "com.tencent.qqlive",      // 腾讯视频
            "com.sohu.sohuvideo",      // 搜狐视频
            "com.qiyi.video",          // 爱奇艺
            "com.youku.phone"          // 优酷
        )

        // 倒计时+跳过 绑定正则
        private val COUNTDOWN_SKIPregex = Regex(
            "跳过.{0,4}[0-9]+|[0-9]+.{0,4}跳过"
        )

        // 防误触下载
        private val DOWNLOAD_ANY = listOf(
            "立即下载", "马上安装", "去下载", "下载安装", "安装应用", "前往应用商店",
            "打开应用商店", "查看详情去下载", "免费下载", "抢先下载", "极速下载"
        )
        private val STATUS_ANY = listOf("下载中", "下载完成", "已下载", "安装完成", "0%", "100%", "%")

        private const val SCORE_WEAK = 40
        private const val SCORE_MID = 70
        private const val SCORE_STRONG = 100
        private const val SCORE_CLICK = 40
    }
}
