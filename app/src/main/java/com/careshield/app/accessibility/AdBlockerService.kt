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
        if (now - cooldown < 400) return
        cooldown = now

        // 【诊断探针】收到事件即打印，用于确认服务在监听及识别链路
        val eventTypeName = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
            else -> "event#${event.eventType}"
        }
        android.util.Log.i("CareShield", "PROBE event=$eventTypeName pkg=${event.packageName} cls=${event.className}")

        val repo = SettingsRepo(this)
        val skipAd = repo.isEnabledSync("func_skip_ad")
        val closePop = repo.isEnabledSync("func_close_pop")
        val swipeAd = repo.isEnabledSync("func_misclick")
        if (!skipAd && !closePop && !swipeAd) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        handleAd(root, skipAd, closePop, swipeAd)
    }

    private fun handleAd(root: AccessibilityNodeInfo, skipAd: Boolean, closePop: Boolean, swipeAd: Boolean) {
        val pkg = packageName ?: return
        val repo = SettingsRepo(this)

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

                val score = scoreNode(combined, text, node, hasAdContext, skipAd, closePop)
                if (score >= SCORE_CLICK) {
                    val target = findClickableAncestor(node) ?: node
                    val source = if (score >= SCORE_STRONG) "广告" else if (skipAd) "广告" else "弹窗"
                    clickIfSafe(target, repo, source)
                    return
                }
            }
        }

        // ---- 策略2: 信息流"广告"标签 → 上滑跳过 ----
        if (swipeAd && AD_LABEL.any { hasTextNode(root, it) }) {
            performSwipeUp()
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

        // 高置信：纯"跳过"作为可点击按钮（开屏广告常见，不依赖广告上下文）
        if (skipAd && node.isClickable && STANDALONE_SKIP.any { combined.trim() == it }) {
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
        try {
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (ok) {
                android.util.Log.i("CareShield", "拦截[$source]: $action (pkg=$pkg)")
                Thread { kotlinx.coroutines.runBlocking { repo.logIntercept(source, action) } }.start()
            }
        } catch (_: Exception) { }
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    @Volatile private var lastSwipeAd = 0L

    companion object {
        // 强证据：明确广告语义，直接拦截
        private val STRONG_SKIP = listOf("跳过广告", "点击跳过广告", "广告可跳过", "播放后跳过")
        private val STRONG_CLOSE = listOf("关闭广告", "关闭弹窗", "关闭广告悬浮窗")

        // 独立"跳过"：作为可点击按钮时视为开屏广告（不依赖广告上下文）
        private val STANDALONE_SKIP = listOf("跳过", "跳過", "Skip", "点击跳过")

        // 中证据：有广告上下文(界面有"广告"标签)时才拦
        private val MID_SKIP = listOf("跳过", "Skip", "点击跳过", "跳過")
        private val MID_CLOSE = listOf("关闭", "关掉", "×", "✕", "✖", "x", "Close", "Dismiss", "Got it", "Not now", "取消", "知道了")

        // 弱证据：裸关闭词，必须 广告上下文 + 角落 + 可点 才拦（防误杀）
        private val WEAK_CLOSE = listOf("×", "✕", "x", "✖", "关闭", "知道了")

        private val AD_LABEL = listOf("广告", "推广", "Sponsored", "Ad")

        // 置信度阈值
        private const val SCORE_WEAK = 40
        private const val SCORE_MID = 70
        private const val SCORE_STRONG = 100
        private const val SCORE_CLICK = 40 // 达到此分数即可点击
    }
}
