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

    /** 应用当前语言（进入 App 时调用） */
    fun applyLocale(context: Context) {
        syncLangState(context)
    }

    /** 切换语言（写入 DataStore + 更新 LangState → 触发全局重建） */
    suspend fun setLocale(context: Context, lang: String) {
        SettingsRepo(context).put("lang", lang)
        LangState.current.value = lang   // 触发 MainActivity 重建
    }
}
