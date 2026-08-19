package com.careshield.app.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.careshield.app.data.SettingsRepo
import com.careshield.app.shizuku.ShizukuManager
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * 流氓软件监听 + 拦截（智能识别）：
 * 流程：新装 APK → 有 Virustotal API key 则云端哈希判定（权威）→ 无 key/失败用本地规则 → 命中则 Shizuku 卸载。
 */
class RogueWatchdog : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED &&
            intent.action != Intent.ACTION_PACKAGE_REPLACED) return
        val pkg = intent.data?.schemeSpecificPart ?: return

        // 跳过自家与 Shizuku
        if (pkg == context.packageName || pkg == "moe.shizuku.privileged.api") return

        // 后台处理（网络/IO 不进主线程）
        Thread {
            handle(context, pkg)
        }.start()
    }

    private fun handle(context: Context, pkg: String) {
        try {
            val repo = SettingsRepo(context)
            val apiKey = repo.getApiKeySync()
            val label = labelOf(context, pkg)

            // 1) 优先：Virustotal 云端（有 key）
            if (apiKey.isNotBlank()) {
                val risk = checkViaVirusTotal(context, pkg, apiKey)
                if (risk != null) {
                    if (risk) {
                        block(context, pkg, label, "Virustotal 高风险")
                    } else {
                        Log.i("CareShield", "云检通过: $pkg")
                    }
                    return
                }
            }

            // 2) 无 key / 云检失败 → 本地规则（预测级 + 危险词库）
            val localRisk = VirusTotalClient.localClassify(label, pkg) ||
                RogueClassifier.classify(pkg, label, false, false, false).action !=
                    RogueClassifier.Action.SKIP
            if (localRisk) {
                block(context, pkg, label, "本地规则命中")
            }
        } catch (_: Exception) { }
    }

    /** 通过 Virustotal 哈希查询，返回 是否恶意；null 表示无法判断（key 无效/网络/未知哈希） */
    private fun checkViaVirusTotal(context: Context, pkg: String, apiKey: String): Boolean? {
        return try {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            val apkFile = File(info.sourceDir)
            if (!apkFile.exists()) return if (isInDangerApps(pkg)) true else false
            val sha = VirusTotalClient.sha256(apkFile) ?: return null
            val result = VirusTotalClient.queryHash(apiKey, sha)
            if (result == null) return null
            if (result.isMalicious) true else false
        } catch (_: Exception) { null }
    }

    private fun isInDangerApps(pkg: String): Boolean {
        // 无法拿到 APK 文件哈希时，回退到危险包名名单前缀匹配（已知危险 App）
        return DANGER_PKG_PREFIXES.any { pkg.contains(it) }
    }

    private fun block(context: Context, pkg: String, label: String, reason: String) {
        Log.i("CareShield", "拦截: $pkg [$label] $reason")
        try {
            // Shizuku 卸载（彻底移除，含安装包）
            val ok = ShizukuManager.execShell("pm uninstall --user 0 $pkg")
            Log.i("CareShield", "卸载 $pkg -> ${ok?.trim()}")
        } catch (_: Exception) { }
        Thread { runBlocking { SettingsRepo(context).logIntercept("流氓", "$pkg $label $reason") } }.start()
    }

    private fun labelOf(context: Context, pkg: String): String {
        return try {
            context.packageManager.getApplicationInfo(pkg, 0).loadLabel(context.packageManager).toString()
        } catch (_: Exception) { pkg }
    }

    companion object {
        // 高危包名片段（本地兜底，云端失败且无法读 APK 时用）
        private val DANGER_PKG_PREFIXES = listOf(
            "com.kuaishou","com.ss.android.ugc.aweme.lite","com.jingdong.app.mall",
            "sogou","qtt","sina.weibo","icoolapp"
        )
    }
}
