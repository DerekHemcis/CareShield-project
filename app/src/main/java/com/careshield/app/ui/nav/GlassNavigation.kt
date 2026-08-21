package com.careshield.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment

data class NavItem(val label: String, val icon: ImageVector?, val emoji: String)

/**
 * 底部导航 —— 采用 Material3 官方 NavigationBar（与 KernelSU / LSPosed 一致的真实组件）
 * - 天然自带选中胶囊指示器、图标切换动画、深浅主题自适应（Material3）
 * - liquidGlass=true: 额外加半透明玻璃背景 + 柔和阴影（贴近 KernelSU 深色玻璃感）
 */
@Composable
fun GlassNavigation(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    liquidGlass: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.3f

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (liquidGlass) Modifier.background(
                    if (dark) Color(0xCC16201C) else Color(0xD9FFFFFF),
                    RoundedCornerShape(26.dp)
                ).padding(horizontal = 6.dp, vertical = 6.dp)
                else Modifier
            ),
        containerColor = if (liquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (liquidGlass) 0.dp else 3.dp,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(index) },
                icon = {
                    // KernelSU 风格：选中图标放大浮起
                    val scale = if (selected) 1.15f else 1f
                    androidx.compose.animation.core.animateFloatAsState(scale).let {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(24.dp)
                                .scale(it.value)
                                .then(if (selected) Modifier.offset(y = (-2).dp) else Modifier)
                        ) {
                            if (item.icon != null) {
                                Icon(item.icon, contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                androidx.compose.material3.Text(item.emoji, fontSize = 20.sp)
                            }
                        }
                    }
                },
                label = {
                    Text(item.label,
                        fontSize = if (selected) 11.sp else 10.sp,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (liquidGlass) {
                        // 液态玻璃胶囊：半透明白/深 + 高光
                        if (dark) Color(0x502E7D5B) else Color(0x66D7F0E4)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    selectedIconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
