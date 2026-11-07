package com.careshield.app.ui.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * 液态玻璃底部导航（Kyant0/AndroidLiquidGlass backdrop 实现）。
 *
 * 结构：
 *  - 外层 Capsule 玻璃面板：vibrancy + blur + lens，绘制在 backdrop 之上
 *  - 选中胶囊：跟随选中项滑动（spring 动画），lens(色散) + Shadow + InnerShadow
 *  - 图标文字层：正常绘制（不透明，保证可读性）
 *
 * 低版本（API < 31 无 RuntimeShader/RenderEffect）时库内部自动跳过对应效果，
 * 仅剩 surface 色块，功能不受影响。
 */
@Composable
fun LiquidGlassNavigation(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    val tabCount = items.size

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidthDp = with(density) {
            ((constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabCount).toDp()
        }
        val tabWidthPx = with(density) { tabWidthDp.toPx() }

        // 选中胶囊位移（spring 滑动）
        val indicatorOffset = remember { Animatable(selectedIndex.toFloat()) }
        LaunchedEffect(selectedIndex) {
            indicatorOffset.animateTo(
                selectedIndex.toFloat(),
                spring(dampingRatio = 0.8f, stiffness = 380f)
            )
        }

        // 按压反馈
        val interactions = remember { List(tabCount) { MutableInteractionSource() } }
        val pressedIndex = pressedIndex(interactions)
        val pressAnim by animateFloatAsState(
            if (pressedIndex >= 0) 1f else 0f, tween(120), label = "press"
        )

        // 玻璃面板
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中胶囊（绝对定位层）
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                Box(
                    Modifier
                        .offset(x = tabWidthDp * indicatorOffset.value)
                        .size(tabWidthDp, 56.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                lens(
                                    10f.dp.toPx() * pressAnim,
                                    14f.dp.toPx() * pressAnim,
                                    chromaticAberration = true
                                )
                            },
                            highlight = { Highlight.Default.copy(alpha = pressAnim) },
                            shadow = { Shadow(alpha = pressAnim) },
                            innerShadow = {
                                InnerShadow(
                                    radius = 8.dp * pressAnim,
                                    color = Color.Black.copy(alpha = 0.2f * pressAnim)
                                )
                            },
                            onDrawSurface = { drawRect(accentColor.copy(alpha = 0.16f)) }
                        )
                )
            }

            // 图标 + 文字（点击热区铺满每格）
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val scale by animateFloatAsState(
                    if (selected) 1.12f else 1f, tween(150), label = "iconScale"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            interactionSource = interactions[index],
                            indication = null
                        ) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (item.icon != null) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            )
                        } else {
                            Text(
                                item.emoji,
                                fontSize = 20.sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                        }
                        Text(
                            item.label,
                            fontSize = if (selected) 11.sp else 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pressedIndex(sources: List<MutableInteractionSource>): Int =
    sources.indexOfFirst { it.collectIsPressedAsState().value }
