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
        // 白名单内的应用不拦截（老人常用）
        val repo = SettingsRepo(this)
        if (repo.isWhitelisted(pkg)) return

        // ---- 策略1: 文字/倒计时/描述识别，点击跳过或关闭 ----
        if (skipAd || closePop) {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            collectAll(root, nodes)
            for (node in nodes) {
                val text = (node.text?.toString() ?: "")
                val desc = (node.contentDescription?.toString() ?: "")
                val combined = "$text $desc"
                if (combined.isBlank()) continue

                val isSkip = skipAd && (SKIP_ANY.any { combined.contains(it) } || AD_LABEL.any { combined.contains(it) })
                val isClose = closePop && CLOSE_ANY.any { combined.contains(it, ignoreCase = true) }
                if (isSkip || isClose) {
                    val target = findClickableAncestor(node) ?: node
                    val source = if (isSkip) "广告" else "弹窗"
                    clickIfSafe(target, repo, source)
                    return
                }
            }
        }

        // ---- 策略2: 信息流"广告"标签 → 上滑跳过（抖音/快手信息流广告） ----
        if (swipeAd && AD_LABEL.any { hasTextNode(root, it) }) {
            // 如果检测到"广告"标签节点，执行一次上滑手势
            performSwipeUp()
        }
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
        val sx = (bounds.left + bounds.width / 2).toFloat()
        val sy1 = (bounds.top + bounds.height * 0.7f)
        val sy2 = (bounds.top + bounds.height * 0.3f)
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
        private val SKIP_ANY = listOf("跳过", "点击跳过", "跳過", "Skip", "跳过广告", "广告可跳过", "播放后跳过")
        private val CLOSE_ANY = listOf("关闭", "关掉", "知道了", "取消", "关闭广告", "关闭弹窗",
            "x", "×", "✕", "✖", "Close", "Dismiss", "Got it", "Not now", "知道了")
        private val COUNTDOWN = listOf("秒", "后跳过", "倒计时", "s 后")
        private val AD_LABEL = listOf("广告", "推广", "Sponsored", "Ad")
    }
}
