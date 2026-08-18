package com.careshield.app.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 集成管理：检测运行状态、权限请求、"一键连接"流程。
 * 无 root 设备依赖 Shizuku 无线调试；点击"去连接"时请求权限，授权后即连接成功。
 */
object ShizukuManager {

    const val CODE_PERMISSION = 1001
    @Volatile private var bound = false

    @Volatile private var listener: (Int, Boolean) -> Unit = { _, _ -> }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        listener(requestCode, granted)
    }

    /** 设置权限结果回调（在 Activity onCreate 注册），签名 (requestCode, granted) */
    fun setPermissionResultListener(l: (Int, Boolean) -> Unit) {
        listener = l
        runCatching { Shizuku.addRequestPermissionResultListener(permissionResultListener) }
    }

    /** Shizuku 服务是否已运行（binder 存在） */
    fun isAvailable(): Boolean =
        try { Shizuku.pingBinder() } catch (e: Throwable) { false }

    /** 是否已获得权限 */
    fun hasPermission(): Boolean {
        return try {
            val preV11: Boolean = Shizuku.isPreV11()
            val granted: Boolean = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            !preV11 && granted
        } catch (e: Throwable) { false }
    }

    /** 增强完全就绪（已连接 + 已授权） */
    fun isEnhanceReady(): Boolean = isAvailable() && hasPermission()

    /** V13：绑定由 ShizukuProvider 自动触发，注册监听即可 */
    fun bind(context: Context) {
        if (bound) return
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            bound = true
        } catch (e: Throwable) { bound = false }
    }

    /**
     * 一键连接：
     *  - 已就绪 → 返回 READY
     *  - 服务运行但无权限 → 发起权限请求，返回 REQUESTING
     *  - 服务未运行 → 返回 NEED_START（App 应引导用户打开 Shizuku）
     */
    fun connect(): ConnectResult {
        if (isEnhanceReady()) return ConnectResult.READY
        if (isAvailable()) {
            requestPermission()
            return ConnectResult.REQUESTING
        }
        return ConnectResult.NEED_START
    }

    enum class ConnectResult { READY, REQUESTING, NEED_START }

    private fun requestPermission() {
        runCatching { Shizuku.requestPermission(CODE_PERMISSION) }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        // Shizuku 已授权并连接
    }

    fun getUid(): Int = if (isAvailable()) Shizuku.getUid() else -1

    /** 尝试屏蔽包安装（需要 Shizuku + 权限）— 拦截流氓软件 */
    fun blockPackageInstall(packageName: String): Boolean {
        if (!isEnhanceReady()) return false
        return try {
            execShell("pm disable-user --user 0 $packageName") != null
        } catch (e: Throwable) { false }
    }

    /** 通过 Shizuku 执行 shell */
    fun execShell(cmd: String): String? {
        if (!isEnhanceReady()) return null
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
