package com.careshield.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.careshield.app.data.SettingsRepo

/**
 * 亲安盾核心：无障碍广告拦截服务
 * 关键词规则库方案：监听界面变化，识别"跳过/关闭/×"等广告按钮自动点击。
 */
class AdBlockerService : AccessibilityService() {

    @Volatile private var cooldown = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 节流：避免频繁触发
        val now = System.currentTimeMillis()
        if (now - cooldown < 350) return
        cooldown = now

        val repo = SettingsRepo(this)
        if (!repo.isEnabledSync("func_skip_ad") && !repo.isEnabledSync("func_close_pop")) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                handleAd(root, repo)
            }
        }
    }

    private fun handleAd(root: AccessibilityNodeInfo, repo: SettingsRepo) {
        val skipAd = repo.isEnabledSync("func_skip_ad")
        val closePop = repo.isEnabledSync("func_close_pop")
        if (!skipAd && !closePop) return

        val clickable = mutableListOf<AccessibilityNodeInfo>()
        collectClickable(root, clickable)

        for (node in clickable) {
            val text = (node.text?.toString() ?: "") + (node.contentDescription?.toString() ?: "")
            if (text.isBlank()) continue
            if (skipAd && SKIP_ANY.any { text.contains(it) }) {
                clickIfSafe(node, repo, "广告"); return
            }
            if (closePop && CLOSE_ANY.any { text.contains(it, ignoreCase = true) }) {
                clickIfSafe(node, repo, "弹窗"); return
            }
        }
    }

    private fun collectClickable(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectClickable(it, out) }
        }
    }

    private fun clickIfSafe(node: AccessibilityNodeInfo, repo: SettingsRepo, source: String) {
        val pkg = packageName ?: return
        if (repo.isWhitelisted(pkg)) return
        try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val action = node.text?.toString() ?: "按钮"
            Thread { kotlinx.coroutines.runBlocking { repo.logIntercept(source, action) } }.start()
        } catch (_: Exception) { }
    }

    override fun onInterrupt() {}

    companion object {
        private val SKIP_ANY = listOf("跳过", "点击跳过", "跳過", "Skip", "跳过广告")
        private val CLOSE_ANY = listOf("关闭", "关掉", "知道了", "取消", "关闭广告",
            "×", "✕", "Close", "Dismiss", "Got it", "Not now")
    }
}
