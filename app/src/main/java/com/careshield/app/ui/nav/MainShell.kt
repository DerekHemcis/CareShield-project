package com.careshield.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.careshield.app.ui.pages.HomeScreen
import com.careshield.app.ui.pages.SettingsScreen
import com.careshield.app.ui.pages.LogScreen
import com.careshield.app.ui.pages.AboutScreen

@Composable
fun MainShell(
    theme: Int,
    onThemeChange: (Int) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenShizuku: () -> Unit,
    shizukuState: Int = 0
) {
    var tab by remember { mutableStateOf(0) }

    val items = listOf(
        NavItem("功能", Icons.Filled.Home, "🏠"),
        NavItem("设置", Icons.Filled.Settings, "⚙️"),
        NavItem("日志", Icons.AutoMirrored.Filled.List, "📋"),
        NavItem("关于", Icons.Filled.Info, "ℹ️"),
    )

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenShizuku = onOpenShizuku,
                    shizukuState = shizukuState
                )
                1 -> SettingsScreen(theme = theme, onThemeChange = onThemeChange)
                2 -> LogScreen()
                3 -> AboutScreen()
            }
        }
        GlassNavigation(
            items = items,
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
