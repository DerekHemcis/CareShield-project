package com.careshield.app.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex

data class NavItem(val label: String, val icon: ImageVector?, val emoji: String)

/**
 * 液体玻璃底部导航栏（自绘，对齐 LiquidGlass-NavBar 视觉）
 *
 * 结构：
 *  - 悬浮"玻璃胶囊容器"：半透明 + 垂直到左高光渐变，边缘圆角大 → 液体玻璃质感
 *  - 内部每个 item 是一个小胶囊：选中时带高光渐变浮起、图标放大、文字变粗变亮
 *  - liquidGlass=true 玻璃质感；false 回退简洁 Material 风格
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
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    // 玻璃容器配色
    val containerTop = if (dark) Color(0xE619221C) else Color(0xF2FFFFFF)
    val containerBottom = if (dark) Color(0xB30E1612) else Color(0xEEF3FAF7)
    // 选中胶囊高光
    val glassTop = if (dark) Color(0x7A3B9A72) else Color(0xCCD7F0E4)
    val glassBottom = if (dark) Color(0x3016382B) else Color(0x80BFE8D4)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // 玻璃容器：顶部有一道横向高光（模拟玻璃反射），主体半透明圆角胶囊
        Box(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(31.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(containerTop, containerBottom)
                    )
                )
        ) {
            // 顶部高光（玻璃折光）
            Canvas(Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to if (dark) Color(0x30FFFFFF) else Color(0x30FFFFFF),
                        0.5f to Color.Transparent
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
            }
            Row(
                Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { i, item ->
                    val selected = i == selectedIndex
                    NavItemChip(
                        item = item,
                        selected = selected,
                        active = active,
                        inactive = inactive,
                        glassTop = glassTop,
                        glassBottom = glassBottom,
                        onClick = { onSelected(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItemChip(
    item: NavItem,
    selected: Boolean,
    active: Color,
    inactive: Color,
    glassTop: Color,
    glassBottom: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        animationSpec = tween(160), label = "scale"
    )
    val iconColor by animateColorAsState(
        if (selected) active else inactive,
        animationSpec = tween(160), label = "icon"
    )
    val lift by animateFloatAsState(
        if (selected) -3f else 0f,
        animationSpec = tween(160), label = "lift"
    )

    Column(
        Modifier
            .width(56.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(
                if (selected)
                    Brush.verticalGradient(listOf(glassTop, glassBottom))
                else Color.Transparent
            )
            .scale(scale)
            .offset(y = lift.dp)
            .zIndex(if (selected) 2f else 0f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (item.icon != null) {
                Icon(item.icon, contentDescription = item.label, tint = iconColor)
            } else {
                Text(item.emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            fontSize = if (selected) 11.sp else 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = iconColor,
            maxLines = 1
        )
    }
}
