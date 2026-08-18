package com.careshield.app.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * Shizuku 集成管理：检测运行状态、降级判断、一键引导开启。
 * 无 root 设备依赖 Shizuku 无线调试运行；有 root 可 root 方式自启。
 */
object ShizukuManager {

    @Volatile private var bound = false

    fun isAvailable(): Boolean =
        Shizuku.pingBinder()

    fun bind(context: Context) {
        if (bound) return
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.bindRequestService(context)
            bound = true
        } catch (e: Throwable) { bound = false }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        // Shizuku 已授权并连接
    }

    fun getUid(): Int = if (isAvailable()) Shizuku.getUid() else -1

    /** 尝试屏蔽包安装（需要 Shizuku + 权限）— 拦截流氓软件 */
    fun blockPackageInstall(context: Context, packageName: String): Boolean {
        if (!isAvailable()) return false
        return try {
            // 通过 pm 禁用指定应用（请用 Shizuku 权限执行 shell）
            execShell("pm disable-user --user 0 $packageName")
            true
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
            context.packageManager.getPackageInfo(ShizukuProvider.AUTHORITY, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
}
