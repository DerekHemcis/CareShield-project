package com.careshield.app.minitest

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * 【最小验证】白板无障碍服务：什么都不做，只在 onServiceConnected 打一行日志。
 * 用于确认系统能否绑定运行一个最基本的 AccessibilityService。
 * 若此服务能收到日志 => 说明系统 bind 正常，问题在 CareShield 工程复杂逻辑。
 */
class MiniA11yService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("MiniA11y", "onServiceConnected pkg=$packageName info=${serviceInfo}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.i("MiniA11y", "onEvent type=${event.eventType} pkg=${event.packageName}")
    }

    override fun onInterrupt() {}
}
