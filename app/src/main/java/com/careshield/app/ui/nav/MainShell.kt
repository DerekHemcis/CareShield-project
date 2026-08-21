package com.careshield.app.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.careshield.app.data.SettingsRepo
import com.careshield.app.ui.locale.T
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
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // 液态玻璃开关（响应式，设置页切换后立即生效）
    val repo = remember { SettingsRepo(ctx) }
    val liquidGlass by repo.observeEnabled("liquid_glass").collectAsState(initial = true)

    val items = listOf(
        NavItem(Icons.Filled.Home,      T("功能")),
        NavItem(Icons.Filled.Settings,  T("设置")),
        NavItem(Icons.AutoMirrored.Filled.List, T("日志")),
        NavItem(Icons.Filled.Info,      T("关于")),
    )

    Column(Modifier.fillMaxSize()) {
        // 主体页面：带动画 + 左右滑动手势切换
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(tab) {
                    var swiped = false
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { swiped = false; acc = 0f },
                        onDragEnd = { }
                    ) { change, dragAmount ->
                        change.consume()
                        if (swiped) return@detectHorizontalDragGestures
                        acc += dragAmount
                        if (acc < -80) {       // 左滑 → 下一tab
                            if (tab < items.size - 1) tab++
                            swiped = true
                        } else if (acc > 80) { // 右滑 → 上一tab
                            if (tab > 0) tab--
                            swiped = true
                        }
                    }
                }
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(animationSpec = androidx.compose.animation.core.tween(150)))
                        .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(100)))
                },
                label = "tabSwitch"
            ) { t ->
                when (t) {
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
        }
        // 底部液态玻璃导航栏（useGlass 响应式绑定开关，设置页切换立即生效）
        LiquidGlassNavBar(
            items = items,
            selectedIndex = tab,
            onSelectedChange = { tab = it },
            useGlass = liquidGlass,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 10.dp),
            selectedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        )
    }
}
