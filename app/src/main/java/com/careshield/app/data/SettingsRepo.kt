package com.careshield.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "careshield_settings")

/**
 * 设置持久化仓库：DataStore 存储 所有开关、白名单、拦截日志、监护人密码、语言、主题。
 */
class SettingsRepo(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.dataStore

    private val KEY_LANG = stringPreferencesKey("lang")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_LIQUID = stringPreferencesKey("liquid")
    private val KEY_GUARDIAN_PASS = stringPreferencesKey("guardian_pass")
    private val KEY_WHITELIST = stringSetPreferencesKey("whitelist_set")
    private val KEY_WHITELIST_MODE = intPreferencesKey("whitelist_mode")
    private val KEY_INTERCEPT_MODE = intPreferencesKey("intercept_mode")
    private val KEY_LOG = stringPreferencesKey("log")
    private val KEY_SHIZUKU = stringPreferencesKey("shizuku_state")

    suspend fun get(key: String, def: String = ""): String {
        val k = stringPreferencesKey(key)
        return dataStore.data.first()[k] ?: def
    }
    suspend fun getInt(key: String, def: Int = 0): Int =
        dataStore.data.first()[intPreferencesKey(key)] ?: def
    suspend fun put(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    // 功能开关（func_xxx = "1"/"0"）
    suspend fun isEnabled(key: String): Boolean = getInt(key, 0) == 1
    suspend fun setEnabled(key: String, enabled: Boolean) {
        dataStore.edit { it[intPreferencesKey(key)] = if (enabled) 1 else 0 }
    }

    // 便捷同步版（无障碍非挂起上下文用）
    fun isEnabledSync(key: String): Boolean {
        return try { runBlocking { isEnabled(key) } } catch (_: Exception) { false }
    }

    fun isWhitelisted(pkg: String): Boolean {
        return try { runBlocking {
            val set = dataStore.data.first()[KEY_WHITELIST] ?: emptySet()
            set.contains(pkg)
        } } catch (_: Exception) { false }
    }

    // 白名单
    suspend fun addWhitelist(pkg: String) {
        dataStore.edit { st -> val s = st[KEY_WHITELIST].orEmpty().toMutableSet(); s.add(pkg); st[KEY_WHITELIST] = s }
    }
    suspend fun removeWhitelist(pkg: String) {
        dataStore.edit { st -> val s = st[KEY_WHITELIST].orEmpty().toMutableSet(); s.remove(pkg); st[KEY_WHITELIST] = s }
    }
    suspend fun getWhitelist(): Set<String> = dataStore.data.first()[KEY_WHITELIST] ?: emptySet()

    // 监护人密码（默认 1234）
    suspend fun getGuardianPass(): String = dataStore.data.first()[KEY_GUARDIAN_PASS] ?: "1234"
    fun getGuardianPassSync(): String = runCatching { runBlocking { getGuardianPass() } }.getOrDefault("1234")
    suspend fun setGuardianPass(pass: String) {
        dataStore.edit { it[KEY_GUARDIAN_PASS] = pass }
    }

    // 白名单保护模式
    suspend fun getWhitelistMode(): Boolean = dataStore.data.first()[KEY_WHITELIST_MODE] == 1
    fun getWhitelistModeSync(): Boolean = runCatching { runBlocking { getWhitelistMode() } }.getOrDefault(false)
    suspend fun setWhitelistMode(on: Boolean) {
        dataStore.edit { it[KEY_WHITELIST_MODE] = if (on) 1 else 0 }
    }

    /** 恢复出厂默认：全部功能关闭、液态关、主题跟随系统(0)、语言简体(简体中文) */
    suspend fun resetDefaults() {
        dataStore.edit { st ->
            listOf("func_skip_ad", "func_close_pop", "func_misclick", "func_block_web",
                "func_rouge", "whitelist_mode", "liquid_glass", "func_aggressive_skip").forEach { k ->
                st[intPreferencesKey(k)] = 0
            }
            st[stringPreferencesKey("theme")] = "0"
            st[stringPreferencesKey("lang")] = "简体中文"
        }
    }

    // ===== 安全 API key（Virustotal，可留空降级为本地规则） =====
    private val KEY_API = stringPreferencesKey("vt_api_key")
    suspend fun getApiKey(): String = dataStore.data.first()[KEY_API] ?: ""
    fun getApiKeySync(): String = runCatching { runBlocking { getApiKey() } }.getOrDefault("")
    suspend fun setApiKey(key: String) {
        dataStore.edit { it[KEY_API] = key }
    }

    // 拦截日志（简单追加，存最近 200 条）
    suspend fun logIntercept(source: String, action: String) {
        dataStore.edit { st ->
            val old = st[KEY_LOG] ?: ""
            val entry = "${System.currentTimeMillis()}|$source|$action\n"
            val combined = old + entry
            st[KEY_LOG] = combined.lines().takeLast(200).joinToString("\n") + "\n"
        }
    }
    suspend fun getLog(): String = get("log", "")

    /** 清空拦截日志 */
    suspend fun clearLog() {
        dataStore.edit { it.remove(KEY_LOG) }
    }


    /** 导出设置为 JSON（用于备份 / 换设备沿用） */
    suspend fun exportSettings(): String {
        val data = dataStore.data.first()
        val sb = StringBuilder("{\n")
        data.asMap().forEach { (key, value) ->
            sb.append("  \"").append(key.name).append("\": \"")
                .append(value.toString().replace("\"", "\\\"")).append("\",\n")
        }
        return sb.toString().trimEnd(',', ' ', '\n') + "\n}"
    }

    /** 从 JSON 备份恢复设置（用于换设备导入） */
    suspend fun importSettings(json: String) {
        dataStore.edit { st ->
            val regex = Regex("\"([^\"]+)\": \"((?:\\\\.|[^\"\\\\])*)\"")
            regex.findAll(json).forEach { m ->
                try {
                    val k = m.groupValues[1]
                    val v = m.groupValues[2].replace("\\\"", "\"")
                    st[stringPreferencesKey(k)] = v
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * 统计拦截计数（按 source 分类）。
     * @return IntArray: [0]=广告, [1]=弹窗, [2]=防装(流氓)
     */
    suspend fun getInterceptCounts(): IntArray {
        val counts = IntArray(3)
        val log = get("log", "")
        if (log.isBlank()) return counts
        for (line in log.lines()) {
            if (line.isBlank()) continue
            // 格式: timestamp|source|action
            val parts = line.split('|')
            if (parts.size < 3) continue
            when (parts[1]) {
                "广告" -> counts[0]++
                "弹窗" -> counts[1]++
                "流氓" -> counts[2]++
            }
        }
        return counts
    }

    companion object {
        // 默认白名单应用（微信、抖音等）
        val DEFAULT_WHITELIST = setOf(
            "com.tencent.mm",    // 微信
            "com.ss.android.ugc.aweme" // 抖音
        )
    }
}
