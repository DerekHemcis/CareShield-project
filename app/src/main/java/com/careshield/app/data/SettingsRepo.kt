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

    companion object {
        // 默认白名单应用（微信、抖音等）
        val DEFAULT_WHITELIST = setOf(
            "com.tencent.mm",    // 微信
            "com.ss.android.ugc.aweme" // 抖音
        )
    }
}
