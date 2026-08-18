package com.careshield.app.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.careshield.app.shizuku.ShizukuManager

/**
 * 流氓软件监听 + 拦截：
 * 监听新安装的应用，用 RogueClassifier 判定，命中拦截类则通过 Shizuku 禁用/卸载。
 */
class RogueWatchdog : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED &&
            intent.action != Intent.ACTION_PACKAGE_REPLACED) return
        val pkg = intent.data?.schemeSpecificPart ?: return

        // 跳过自家与 Shizuku
        if (pkg == context.packageName || pkg == "moe.shizuku.privileged.api") return

        // 提取信息 + 判定
        val verdict = evaluatePackage(context, pkg)
        if (verdict.action == RogueClassifier.Action.BLOCK || verdict.action == RogueClassifier.Action.WARN) {
            Log.i("CareShield", "流氓拦截: $pkg [${verdict.category}] ${verdict.reason}")
            // 通过 Shizuku 禁用（防流氓最有效、可逆）
            try {
                val ok = ShizukuManager.execShell("pm disable-user --user 0 $pkg")
                Log.i("CareShield", "已禁用 $pkg -> ${ok?.trim()}")
            } catch (_: Exception) { }
            // 记录
            Thread { kotlinx.coroutines.runBlocking {
                com.careshield.app.data.SettingsRepo(context).logIntercept("流氓", "$pkg ${verdict.reason}")
            } }.start()
        }
    }

    private fun evaluatePackage(context: Context, pkg: String): RogueClassifier.Verdict {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(pkg, PackageManager.GET_PERMISSIONS)
            val label = info.loadLabel(pm).toString()
            // 权限提取
            val perms = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            val hasDeviceAdmin = perms.any { it.contains("BIND_DEVICE_ADMIN") }
            val hasSms = perms.any { it.contains("SEND_SMS") || it.contains("READ_SMS") }
            val hasAccessibility = perms.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }
            RogueClassifier.classify(pkg, label, hasDeviceAdmin, hasAccessibility, hasSms)
        } catch (_: Exception) {
            RogueClassifier.Verdict(RogueClassifier.Action.SKIP, 0, "无法读取")
        }
    }
}
