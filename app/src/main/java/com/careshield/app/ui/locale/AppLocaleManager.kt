package com.careshield.app.ui.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.careshield.app.data.SettingsRepo
import java.util.Locale

/**
 * 应用语言管理：使用 AndroidX per-app 语言 API（零重启即时切换）
 * 语言标识存 DataStore，切换时调用 AppCompatDelegate.setApplicationLocales
 */
object AppLocaleManager {

    /** 支持的 locale，key 对应 DataStore 存的语言名 */
    private val LOCALES = mapOf(
        "简体中文" to "zh",
        "繁體中文" to "zh-Hant",
        "English" to "en"
    )

    val LOCALE_OPTIONS = listOf("简体中文", "繁體中文", "English")

    /** 应用当前语言（每次进入 App 或恢复默认时调用） */
    fun applyLocale(context: Context) {
        val lang = runCatching {
            kotlinx.coroutines.runBlocking { SettingsRepo(context).get("lang", "简体中文") }
        }.getOrDefault("简体中文")
        val tag = LOCALES[lang] ?: "zh"
        try {
            AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(tag + if (tag == "zh-Hant") "" else "")
            )
        } catch (_: Exception) { }
    }

    /** 切换语言（写入 DataStore + 应用） */
    suspend fun setLocale(context: Context, lang: String) {
        SettingsRepo(context).put("lang", lang)
        val tag = LOCALES[lang] ?: "zh"
        AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(tag)
        )
    }
}
