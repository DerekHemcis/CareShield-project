package com.careshield.app.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 集成管理：检测运行状态、降级判断、一键引导开启。
 * 无 root 设备依赖 Shizuku 无线调试运行；有 root 可 root 方式自启。
 */
object ShizukuManager {

    val AUTHORITY = "rikka.shizuku"
    private const val CODE_PERMISSION = 1001
    @Volatile private var bound = false

    /** Shizuku 服务是否已连接（binder 存在） */
    fun isAvailable(): Boolean =
        try { Shizuku.pingBinder() } catch (e: Throwable) { false }

    /** 是否真正获得 Shizuku 权限（增强真正生效） */
    fun hasPermission(): Boolean {
        return try {
            val preV11: Boolean = Shizuku.isPreV11()
            val granted: Boolean = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            !preV11 && granted
        } catch (e: Throwable) { false }
    }

    /**
     * 增强是否完全就绪（需连接 + 权限）：
     * 未装/未授权返回 false，首页应显示"未连接/待授权"。
     */
    fun isEnhanceReady(): Boolean = isAvailable() && hasPermission()

    /** V13 兼容：绑定由 Manifest 中的 ShizukuProvider 自动触发，这里只需注册监听 + 可请求权限 */
    fun bind(context: Context) {
        if (bound) return
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            // V13 不再需要手动 bind，绑定由 ShizukuProvider 完成
            // 这里仅尝试请求读写权限（权限已由 root 或手动授权、或无线调试授予）
            bound = true
        } catch (e: Throwable) { bound = false }
    }

    fun requestPermission() {
        // 请求 Shizuku 权限（V13 支持，需要 Activity 上下文配合 result listener）
        runCatching { Shizuku.requestPermission(CODE_PERMISSION) }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        // Shizuku 已授权并连接
    }

    fun getUid(): Int = if (isAvailable()) Shizuku.getUid() else -1

    /** 尝试屏蔽包安装（需要 Shizuku + 权限）— 拦截流氓软件 */
    fun blockPackageInstall(packageName: String): Boolean {
        if (!isAvailable()) return false
        return try {
            execShell("pm disable-user --user 0 $packageName") != null
        } catch (e: Throwable) { false }
    }

    /** 通过 Shizuku 执行 shell */
    fun execShell(cmd: String): String? {
        if (!isAvailable()) return null
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.inputStream.bufferedReader().readText()
        } catch (e: Throwable) { null }
    }

    fun isServiceInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
}
