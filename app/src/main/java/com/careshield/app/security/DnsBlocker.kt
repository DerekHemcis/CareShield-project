package com.careshield.app.security

import android.content.Context
import com.careshield.app.shizuku.ShizukuManager

/**
 * DNS/域名拦截（B 路线：从 AdGuard 规则提取的域名黑名单，经 Shizuku 写入 /etc/hosts）
 * 需 root/Shizuku。拦截广告域名解析，从源头减少广告下载/跳转。
 */
object DnsBlocker {

    private const val HOSTS_PATH = "/system/etc/hosts"
    private const val MARK_START = "# CareShield-AD-BLOCK-START"
    private const val MARK_END = "# CareShield-AD-BLOCK-END"

    /** host 是否已启用（文件内有我们的标记） */
    fun isEnabled(): Boolean {
        val hosts = ShizukuManager.execShell("cat $HOSTS_PATH") ?: return false
        return hosts.contains(MARK_START)
    }

    /** 启用：备份原 hosts → 写入广告域名 block */
    fun enable(context: Context): Boolean {
        if (!ShizukuManager.isEnhanceReady()) return false
        return try {
            val block = loadDomains(context)
            // 备份原 hosts
            ShizukuManager.execShell("cp $HOSTS_PATH /sdcard/hosts.bak 2>/dev/null")
            // 构造新 hosts：原内容删除旧的 block 段 + 追加新 block
            val orig = ShizukuManager.execShell("cat $HOSTS_PATH") ?: ""
            val cleaned = removeMarkSection(orig)
            val content = cleaned + "\n$MARK_START\n" + block + "\n$MARK_END\n"
            // 写入（需 root 权限，用 su/直接写）
            val tmp = "/data/local/tmp/careshield_hosts"
            sh("echo '$content' > $tmp 2>/dev/null")
            // 用 shizuku root shell 写 system 等
            sh("cat $tmp > $HOSTS_PATH 2>/dev/null")
            true
        } catch (_: Exception) { false }
    }

    /** 禁用：恢复备份或删除 block 段 */
    fun disable(): Boolean {
        return try {
            val orig = ShizukuManager.execShell("cat ${HOSTS_PATH}_orig 2>/dev/null")
                ?: ShizukuManager.execShell("cat /sdcard/hosts.bak 2>/dev/null")
            if (orig != null && orig.isNotBlank()) {
                sh("echo '${orig.replace("'", "'\\''")}' > $HOSTS_PATH 2>/dev/null")
            } else {
                val cur = ShizukuManager.execShell("cat $HOSTS_PATH") ?: ""
                val cleaned = removeMarkSection(cur)
                sh("echo '${cleaned.replace("'", "'\\''")}' > $HOSTS_PATH 2>/dev/null")
            }
            true
        } catch (_: Exception) { false }
    }

    private fun sh(cmd: String) {
        ShizukuManager.execShell(cmd)
    }

    private fun loadDomains(context: Context): String {
        // 从 raw/ad_hosts.txt 读域名黑名单
        return try {
            val txt = context.resources.openRawResource(com.careshield.app.R.raw.ad_hosts)
                .bufferedReader().readText()
            txt.trim()
        } catch (_: Exception) { "" }
    }

    private fun removeMarkSection(content: String): String {
        val start = content.indexOf(MARK_START)
        val end = content.indexOf(MARK_END)
        if (start >= 0 && end >= start) {
            return content.removeRange(start, end + MARK_END.length + 2)
        }
        return content
    }
}
