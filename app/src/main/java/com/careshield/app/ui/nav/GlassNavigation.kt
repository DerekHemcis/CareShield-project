package com.careshield.app.ui.nav

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.careshield.app.R

data class NavItem(
    val label: String,
    val icon: ImageVector?,
    val emoji: String
)

/**
 * KernelSU 风格液态玻璃底部导航
 * 玻璃条 + 可滑动胶囊 + 选中图标浮起。API31+ 可用 AGSL 增强（本组件以 Compose 渐变近似，保证全版本可编译）
 */
@Composable
fun GlassNavigation(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(bg, bg.copy(alpha = 0.35f))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                // 选中胶囊：流动到该 tab 下方
                if (selected) {
                    Box(
                        Modifier
                            .size(width = 52.dp, height = 30.dp)
                            .shadow(6.dp, RoundedCornerShape(15.dp))
                            .background(
                                if (MaterialTheme.colorScheme.background.luminance() < 0.3f)
                                    Brush.verticalGradient(listOf(primary.copy(alpha=0.55f), primary.copy(alpha=0.25f)))
                                else Brush.verticalGradient(listOf(primary.copy(alpha=0.30f), primary.copy(alpha=0.12f))),
                                RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        NavIcon(item, selected, primary)
                    }
                } else {
                    Box(
                        Modifier.size(width = 52.dp, height = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NavIcon(item, selected, primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(item: NavItem, selected: Boolean, primary: Color) {
    val scale by animateFloatAsState(if (selected) 1.28f else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
    val color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        Box(Modifier.scale(scale)) {
            if (item.icon != null) {
                Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(22.dp))
            } else {
                Text(item.emoji, fontSize = 22.sp)
            }
        }
        if (selected) {
            Text(
                item.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = 0.dp, y = (-6).dp)
            )
        } else {
            Text(item.label, fontSize = 10.sp, color = textColor, modifier = Modifier.offset(y = (-6).dp))
        }
    }
}
