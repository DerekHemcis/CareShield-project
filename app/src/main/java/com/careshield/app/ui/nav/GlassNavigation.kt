package com.careshield.app.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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

data class NavItem(
    val label: String,
    val icon: ImageVector?,
    val emoji: String
)

/**
 * 底部导航（玻璃条 + 胶囊高亮 + 图标浮起）
 * 每个 tab 均可点击触发 onSelected
 */
@Composable
fun GlassNavigation(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.3f
    val bg = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(66.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.35f))),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().height(54.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                // 每个 tab 都可点击，切换页面
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    // 选中胶囊（仅视觉，位置固定居中）
                    val capsuleAlpha by animateFloatAsState(
                        if (selected) 1f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    val capGradient = if (dark)
                        Brush.verticalGradient(listOf(
                            primary.copy(alpha = 0.50f + 0.25f * capsuleAlpha),
                            primary.copy(alpha = 0.20f + 0.2f * capsuleAlpha)
                        ))
                    else Brush.verticalGradient(listOf(
                        primary.copy(alpha = 0.28f + 0.25f * capsuleAlpha),
                        primary.copy(alpha = 0.10f + 0.2f * capsuleAlpha)
                    ))
                    Box(
                        Modifier
                            .size(width = 50.dp, height = 30.dp)
                            .shadow(6.dp, RoundedCornerShape(15.dp))
                            .background(capGradient, RoundedCornerShape(15.dp))
                    )
                    // 图标 + 文字（选中浮起放大）
                    NavIcon(item, selected, if (selected) Color.White else onSurface)
                }
            }
        }
    }
}

@Composable
private fun NavIcon(item: NavItem, selected: Boolean, colorTint: Color) {
    val scale by animateFloatAsState(
        if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    val labelVisible by animateFloatAsState(if (selected) 1f else 0f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.scale(scale)) {
            if (item.icon != null) {
                Icon(item.icon, contentDescription = item.label, tint = colorTint, modifier = Modifier.size(22.dp))
            } else {
                Text(item.emoji, fontSize = 22.sp)
            }
        }
        // 文字只显示给选中的项，避免重叠
        if (selected) {
            Text(
                item.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        } else {
            Text(item.label, fontSize = 9.sp, color = colorTint.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}
