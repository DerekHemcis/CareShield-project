package com.careshield.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.CareShieldTheme
import com.careshield.app.ui.nav.MainShell
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            var theme by remember { mutableStateOf(0) }
            val repo = SettingsRepo(this)
            // 读取设置
            LaunchedEffect(Unit) {
                theme = repo.get("theme", "0").toIntOrNull() ?: 0
            }
            CareShieldTheme(theme) {
                MainShell(
                    theme = theme,
                    onThemeChange = { t -> theme = t; scope.launch { repo.put("theme", t.toString()) } },
                    onOpenAccessibility = { openAccessibility(this@MainActivity) },
                    onOpenShizuku = { openShizuku(this@MainActivity) }
                )
            }
        }
    }

    private fun openAccessibility(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) { }
    }
    private fun openShizuku(ctx: Context) {
        runCatching { ctx.startActivity(Intent("moe.shizuku.manager.intent.action.API_V2")) }
    }
}
