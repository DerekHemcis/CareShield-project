package com.careshield.app.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.careshield.app.ui.BgDark
import com.careshield.app.ui.BgLight
import com.careshield.app.ui.CouiColors
import com.careshield.app.ui.UiStyle
import com.careshield.app.ui.currentUiStyle
import com.careshield.app.ui.locale.T
import com.careshield.app.ui.pages.AboutScreen
import com.careshield.app.ui.pages.HomeScreen
import com.careshield.app.ui.pages.LogScreen
import com.careshield.app.ui.pages.SettingsScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun MainShell(
    theme: Int,
    onThemeChange: (Int) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenShizuku: () -> Unit,
    shizukuState: Int = 0,
    onStyleChange: (UiStyle) -> Unit = {}
) {
    var tab by remember { mutableStateOf(0) }
    val style = currentUiStyle()

    val items = listOf(
        NavItem(T("功能"), Icons.Filled.Home, "🏠"),
        NavItem(T("设置"), Icons.Filled.Settings, "⚙️"),
        NavItem(T("日志"), Icons.AutoMirrored.Filled.List, "📋"),
        NavItem(T("关于"), Icons.Filled.Info, "ℹ️"),
    )

    // 液态玻璃 backdrop：底色 + 主体内容一并画入（导航栏玻璃取材于此）
    val dark = theme == 2 || (theme == 0 && isSystemInDarkTheme())
    val bg = if (style == UiStyle.COUI) {
        if (dark) CouiColors.BgDark else CouiColors.BgLight
    } else {
        if (dark) BgDark else BgLight
    }
    val backdrop = rememberLayerBackdrop {
        drawRect(bg)
        drawContent()
    }

    Column(Modifier.fillMaxSize().background(bg)) {
        // 主体页面：带动画 + 左右滑动手势切换；内容画入 backdrop 供玻璃采样
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .layerBackdrop(backdrop)
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
                            if (tab < 3) tab++
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
                    // 快速淡入淡出（避免卡顿）
                    (fadeIn(animationSpec = tween(150)))
                        .togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "tabSwitch"
            ) { t ->
                when (t) {
                    0 -> HomeScreen(
                        onOpenAccessibility = onOpenAccessibility,
                        onOpenShizuku = onOpenShizuku,
                        shizukuState = shizukuState
                    )
                    1 -> SettingsScreen(
                        theme = theme,
                        onThemeChange = onThemeChange,
                        onStyleChange = onStyleChange
                    )
                    2 -> LogScreen()
                    3 -> AboutScreen()
                }
            }
        }
        // 底部导航：COUI=液态玻璃（backdrop 实现），Material3=标准 NavigationBar（玻璃底色）
        if (style == UiStyle.COUI) {
            LiquidGlassNavigation(
                items = items,
                selectedIndex = tab,
                onSelected = { tab = it },
                backdrop = backdrop,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            GlassNavigation(
                items = items,
                selectedIndex = tab,
                onSelected = { tab = it },
                liquidGlass = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
