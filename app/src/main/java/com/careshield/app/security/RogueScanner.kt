package com.careshield.app.security

import android.content.Context
import android.content.pm.PackageManager

/**
 * 主动扫描手机内已安装应用，识别"流氓/风险"App 并可选一键清理（Shizuku 卸载）。
 *
 * 与 RogueWatchdog（依赖 PACKAGE_ADDED 广播，安卓8+ 后台收不到）不同，
 * 这是由用户手动触发（前台扫描），不依赖后台广播，可靠且可控。
 *
 * 用途：首页/设置页"一键扫描手机"→ 列出高风险 App → 用户确认后用 Shizuku 卸载。
 */
object RogueScanner {

    /** 单个扫描结果 */
    data class ScanItem(
        val packageName: String,
        val label: String,
        val category: Int,          // 1色情 2赌博 3贷款 4私彩 5病毒 6流氓 7不明
        val reason: String,
        val isSystem: Boolean
    )

    /**
     * 扫描已安装应用，返回所有"需要拦截"(BLOCK) 且非系统关键应用的结果。
     * 系统关键应用(true system / 平台签名)默认跳过，避免误卸重要组件。
     */
    fun scan(context: Context): List<ScanItem> {
        val pm = context.packageManager
        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) { return emptyList() }

        val result = mutableListOf<ScanItem>()
        for (app in apps) {
            val pkg = app.packageName ?: continue
            // 跳过 CareShield 自己、Shizuku、Google 系统框架
            if (pkg == context.packageName) continue
            if (pkg == "moe.shizuku.privileged.api") continue
            // 跳过系统自带/平台应用（避免误卸）
            val isSystem = isSystemApp(app)
            if (isSystem && !isSuspiciousSystemPkg(pkg)) continue

            val label = try {
                app.loadLabel(context.packageManager).toString()
            } catch (_: Exception) { pkg }

            // 用本地规则判定（与防装同一套）：
            val verdict = RogueClassifier.classify(
                pkg, label,
                hasDeviceAdmin = false,
                hasAccessibility = false,
                hasSms = false
            )
            if (verdict.action == RogueClassifier.Action.BLOCK) {
                result.add(ScanItem(pkg, label, verdict.category, verdict.reason, isSystem))
            }
        }
        return result
    }

    private fun isSystemApp(app: android.content.pm.ApplicationInfo): Boolean {
        return try {
            (app.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or
                android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (_: Exception) { false }
    }

    /** 即便是系统 App，如果包名命中高危词也值得列出（如某些流氓内置） */
    private fun isSuspiciousSystemPkg(pkg: String): Boolean {
        return listOf("gamble", "loan", "lotto", "bet", "casino", "porn", "sex")
            .any { pkg.lowercase().contains(it) }
    }
}
