package com.careshield.app.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(val label: String, val icon: ImageVector?, val emoji: String)

/**
 * KernelSU 风格底部导航
 * liquidGlass=true → 液态玻璃（毛玻璃渐变+高光+选中胶囊，深浅自适应）
 * liquidGlass=false → 简约风格（扁平底色+普通高亮）
 */
@Composable
fun GlassNavigation(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    liquidGlass: Boolean = true,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.3f

    if (liquidGlass) {
        LiquidGlassNav(items, selectedIndex, onSelected, primary, dark, modifier)
    } else {
        MinimalNav(items, selectedIndex, onSelected, primary, dark, modifier)
    }
}

/** 液态玻璃导航（KernelSU 风格） */
@Composable
private fun LiquidGlassNav(
    items: List<NavItem>, selectedIndex: Int, onSelected: (Int) -> Unit,
    primary: Color, dark: Boolean, modifier: Modifier
) {
    // 玻璃底：深色深玻璃/浅色浅玻璃
    val glassBase = if (dark) Brush.linearGradient(listOf(Color(0xFF1E2A26), Color(0xFF14201C)))
        else Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xB3FFFFFF)))
    val onItem = if (dark) Color(0xFF9FB5AC) else Color(0xFF5A6B64)
    val capColor = if (dark) Color(0xFF2E7D5B) else Color(0xFFD7F0E4)
    val capAccent = if (dark) Color(0xFF55D9A8) else Color(0xFF2EAF7C)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(70.dp)
            .shadow(12.dp, RoundedCornerShape(30.dp))
            .background(glassBase, RoundedCornerShape(30.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    // 选中胶囊（液态玻璃高光）
                    val capsuleAlpha by animateFloatAsState(
                        if (selected) 1f else 0f,
                        animationSpec = tween(250)
                    )
                    Box(
                        Modifier
                            .size(width = 52.dp, height = 32.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(listOf(
                                    capColor.copy(alpha = 0.55f + 0.4f * capsuleAlpha),
                                    capColor.copy(alpha = 0.3f + 0.4f * capsuleAlpha)
                                )),
                                RoundedCornerShape(16.dp)
                            )
                            .then(if (dark) Modifier else Modifier)
                    )
                    // 图标 + 文字
                    NavIcon(item, selected, if (selected) capAccent else onItem)
                }
            }
        }
    }
}

/** 简约导航（关闭液态玻璃后，KernelSU 关闭效果：扁平底色 + 普通高亮） */
@Composable
private fun MinimalNav(
    items: List<NavItem>, selectedIndex: Int, onSelected: (Int) -> Unit,
    primary: Color, dark: Boolean, modifier: Modifier
) {
    val bg = if (dark) Color(0xFF171E1C) else Color(0xFFFFFFFF)
    val onItem = if (dark) Color(0xFF8FA39B) else Color(0xFF6B7A74)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(66.dp)
            .background(bg, RoundedCornerShape(0.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().height(54.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (item.icon != null) Icon(item.icon, contentDescription = item.label,
                            tint = if (selected) primary else onItem, modifier = Modifier.size(24.dp))
                        else Text(item.emoji, fontSize = 20.sp)
                        Text(item.label, fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) primary else onItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(item: NavItem, selected: Boolean, color: Color) {
    val scale by animateFloatAsState(
        if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.7f)
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.scale(scale)) {
            if (item.icon != null) Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(22.dp))
            else Text(item.emoji, fontSize = 20.sp)
        }
        Text(item.label, fontSize = if (selected) 11.sp else 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else color)
    }
}
