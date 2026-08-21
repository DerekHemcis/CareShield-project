package com.careshield.app.ui.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

/**
 * 可复用的「液态玻璃」底部导航栏 —— 与具体页面 / 导航框架完全解耦。
 *
 * 磨砂玻璃栏 + 可水平拖拽并自动吸附到最近图标的玻璃药丸指示器 + 跟随指示器的镜面高光。
 * 标准 Compose API 实现（animateFloatAsState 驱动 + detectHorizontalDragGestures）。
 * 不拥有任何页面，只负责"选中了第几项"，由宿主（App）自行决定切到哪个页面。
 *
 * @param items         导航项（图标 + 文字）。指示器宽度按 `70 * 3.5/count` 自适应。
 * @param selectedIndex 当前选中项，由宿主导航状态驱动。
 * @param onSelectedChange 点击 / 拖拽吸附后回写，宿主据此切页。
 * @param selectedColor 选中色，默认绿（可传主题色）。
 * @param unselectedColor 未选中色。
 * @param useGlass      是否玻璃效果（false 回退纯色 Material 背景）。
 * @param onDragParallax 可选拖拽视差回调（px）。不传则无视差。
 */
@Composable
fun LiquidGlassNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color(0xFF4CAF50),
    unselectedColor: Color = Color.White,
    useGlass: Boolean = true,
    onDragParallax: ((Float) -> Unit)? = null,
) {
    val density = LocalDensity.current

    // 导航栏宽度(dp)，onSizeChanged 时更新；指示器中心(dp)
    var navbarWidthDp by remember { mutableFloatStateOf(0f) }
    // 拖拽时被临时改动的指示器中心；未拖拽时由 animateFloatAsState 平滑跟随选中项
    var dragCenterDp by remember { mutableFloatStateOf(-1f) }

    val count = items.size
    // 指示器宽度：与原项目一致，70 * (3.5/count).coerceIn(1,1.2)；4 项 → 70dp
    val indicatorWidthDp = 70f * (3.5f / count).coerceIn(1f, 1.2f)

    fun centerOf(index: Int): Float =
        if (navbarWidthDp > 0f) navbarWidthDp / count * (index + 0.5f) else 0f

    // 目标中心：拖拽中用 dragCenterDp，否则选中项中心
    val targetCenter = if (dragCenterDp >= 0f) dragCenterDp else centerOf(selectedIndex).coerceIn(indicatorWidthDp / 2f, max(indicatorWidthDp / 2f, navbarWidthDp - indicatorWidthDp / 2f))

    // 指示器中心随目标平滑动画（FastOutSlowIn）
    val animatedCenter by animateFloatAsState(
        targetValue = targetCenter,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "indicatorCenter"
    )
    // 用于绘制镜面高光的当前中心
    val displayCenter = if (dragCenterDp >= 0f) dragCenterDp else animatedCenter

    Box(
        modifier
            .fillMaxWidth()
            .height(70.dp)
            .onSizeChanged { navbarWidthDp = with(density) { it.width.toDp().value } }
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(35.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(35.dp))
            .then(
                if (useGlass)
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.30f),
                            0.18f to Color.White.copy(alpha = 0.16f),
                            1f to Color.White.copy(alpha = 0.08f),
                        ),
                    )
                else Modifier.background(MaterialTheme.colorScheme.surface)
            )
            .border(
                width = if (useGlass) 1.dp else 0.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(35.dp),
            )
            .pointerInput(Unit) {
                var dragStartCenter = 0f
                var dragStartPointerX = 0f
                var dragged = false
                fun nearestIndex(centerDp: Float): Int {
                    var best = 0
                    var bestDist = Float.MAX_VALUE
                    for (k in 0 until count) {
                        val d = abs(centerOf(k) - centerDp)
                        if (d < bestDist) { bestDist = d; best = k }
                    }
                    return best
                }
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        dragStartCenter = displayCenter
                        dragStartPointerX = startOffset.x
                        dragged = false
                    },
                    onDragEnd = {
                        val nearest = nearestIndex(displayCenter)
                        if (dragged) onSelectedChange(nearest)
                        dragCenterDp = -1f
                        onDragParallax?.invoke(0f)
                    },
                    onDragCancel = {
                        val nearest = nearestIndex(displayCenter)
                        onSelectedChange(nearest)
                        dragCenterDp = -1f
                        onDragParallax?.invoke(0f)
                    },
                    onHorizontalDrag = { change, _ ->
                        val dx = change.position.x - dragStartPointerX
                        if (abs(dx) > 8f) dragged = true
                        val dxDp = with(density) { dx.toDp().value }
                        dragCenterDp = (dragStartCenter + dxDp)
                            .coerceIn(indicatorWidthDp / 2f, max(indicatorWidthDp / 2f, navbarWidthDp - indicatorWidthDp / 2f))
                        onDragParallax?.invoke(dx)
                    },
                )
            },
    ) {
        // 跟随指示器的镜面高光（模拟 liquid glass 光泽）
        val specXpx = with(density) { displayCenter.dp.toPx() }
        val navWpx = with(density) { navbarWidthDp.dp.toPx() }
        val clampedSpecX = specXpx.coerceIn(0f, navWpx)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        radius = 110f,
                        center = Offset(clampedSpecX, 0f),
                        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )

        // 图标 + 文字
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val isSel = index == selectedIndex
                Column(
                    Modifier
                        .clickable { onSelectedChange(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSel) selectedColor else unselectedColor,
                        modifier = Modifier.size(if (isSel) 28.dp else 24.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        color = if (isSel) selectedColor else unselectedColor,
                        fontSize = (if (isSel) 12 else 10).sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // 可拖拽的玻璃药丸指示器（悬浮于栏上方约 15dp）
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (displayCenter - indicatorWidthDp / 2f).dp, y = (-15).dp)
                .width(indicatorWidthDp.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(((indicatorWidthDp / 2f)).dp))
                .background(Color.White.copy(alpha = 0.20f))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(((indicatorWidthDp / 2f)).dp)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.5f),
                            0.5f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

/** 导航项数据类（图标 + 文字）。 */
data class NavItem(
    val icon: ImageVector,
    val label: String,
)
