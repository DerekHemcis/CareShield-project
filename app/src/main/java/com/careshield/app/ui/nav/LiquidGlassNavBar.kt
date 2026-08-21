package com.careshield.app.ui.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 可复用的「液态玻璃」底部导航栏 —— 与具体页面 / 导航框架完全解耦。
 *
 * 这是从 Flutter 版 LiquidGlass-NavBar 转写而来，效果一致：
 * 磨砂玻璃栏 + 可水平拖拽并自动吸附到最近图标的玻璃药丸指示器 + 跟随指针的镜面高光。
 * 它**不拥有任何页面**，只负责"选中了第几项"，由宿主（你的 App）自行决定切到哪个页面。
 *
 * 典型用法（4 项导航栏）：
 * ```
 * val items = listOf(
 *     NavItem(Icons.Filled.Home,         "Home"),
 *     NavItem(Icons.Filled.Search,       "Search"),
 *     NavItem(Icons.Filled.Notifications,"Alerts"),
 *     NavItem(Icons.Filled.Person,       "Profile"),
 * )
 * var selected by remember { mutableIntStateOf(0) }
 *
 * LiquidGlassNavBar(
 *     items          = items,
 *     selectedIndex  = selected,
 *     onSelectedChange = { selected = it },
 *     modifier = Modifier
 *         .align(Alignment.BottomCenter)
 *         .padding(bottom = 20.dp, start = 15.dp, end = 15.dp),
 * )
 * ```
 * - 若用 Navigation Compose：在 onSelectedChange 里 `navController.navigate(route)`。
 * - 若用 HorizontalPager：在 onSelectedChange 里 `pagerState.animateScrollToPage(it)`，
 *   并可传 onDragParallax 实现页面视差（见参数说明）。
 *
 * @param items          导航项（图标 + 文字）。支持任意数量；4 项直接传 4 个即可，
 *                       指示器宽度会自动按 `70 * (3.5 / count)` 适配。
 * @param selectedIndex  当前选中项，**由宿主的导航状态驱动**（点击/拖拽最终会回写到这里）。
 * @param onSelectedChange 点击某项、或拖拽松手吸附到某项时回调，宿主据此切换页面。
 * @param modifier       外部布局（建议加 padding；用 Box 的 align 控制位置）。
 * @param selectedColor  选中色，默认绿；传你 App 的主题色即可。
 * @param unselectedColor 未选中色，默认白。
 * @param onDragParallax 可选：拖拽过程中的视差回调，参数是「指针相对拖拽起点的水平位移(px)」。
 *                       宿主若用 Pager，可据此做 `dragOffset * 0.25` 的页面视差。不传则无视差。
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
    val scope = rememberCoroutineScope()

    var navbarWidthDp by remember { mutableFloatStateOf(0f) }
    var displayCenter by remember { mutableFloatStateOf(0f) } // 指示器中心(dp)，拖拽时即时更新
    var snapJob by remember { mutableStateOf<Job?>(null) }

    val count = items.size
    // 与原项目一致：indicatorWidth = 70 * (3.5 / count).clamp(下限1, 上限1.2)
    // 3 项→约 81.7dp；4 项及以上→70dp（被下限钳到 1.0 → 70dp）。
    val indicatorWidthDp = 70f * (3.5f / count).coerceIn(1f, 1.2f)

    fun centerOf(i: Int): Float =
        if (navbarWidthDp > 0f) navbarWidthDp / count * (i + 0.5f) else 0f

    // 宽度确定 / 外部选中态变化 → 把指示器平滑移到对应项中心
    LaunchedEffect(navbarWidthDp, selectedIndex) {
        if (navbarWidthDp > 0f) {
            snapJob?.cancel()
            snapJob = scope.launch {
                animate(displayCenter, centerOf(selectedIndex), tween(360, easing = FastOutSlowInEasing)) { v, _ ->
                    displayCenter = v
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(70.dp)
            .onSizeChanged { navbarWidthDp = with(density) { it.width.toDp().value } }
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(35.dp),
                ambientColor = Color.Black.copy(0.35f),
                spotColor = Color.Black.copy(0.25f),
            )
            .clip(RoundedCornerShape(35.dp))
            .then(
                if (useGlass)
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(0.30f),
                            0.18f to Color.White.copy(0.16f),
                            1f to Color.White.copy(0.08f),
                        ),
                    )
                else Modifier.background(
                    androidx.compose.material3.MaterialTheme.colorScheme.surface,
                )
            )
            .border(
                if (useGlass) 1.dp else 0.dp,
                Color.White.copy(0.35f),
                RoundedCornerShape(35.dp)
            )
            .pointerInput(Unit) {
                var startCenter = 0f
                var startPointerX = 0f
                var dragged = false
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        snapJob?.cancel()
                        startCenter = displayCenter
                        startPointerX = offset.x
                        dragged = false
                    },
                    onDragEnd = {
                        snapJob?.cancel()
                        val nearest = (0 until count).minByOrNull { k -> abs(centerOf(k) - displayCenter) } ?: 0
                        if (dragged) onSelectedChange(nearest)
                        snapJob = scope.launch {
                            animate(displayCenter, centerOf(nearest), tween(420, easing = FastOutSlowInEasing)) { v, _ ->
                                displayCenter = v
                            }
                        }
                        onDragParallax?.invoke(0f)
                    },
                    onDragCancel = {
                        snapJob?.cancel()
                        val nearest = (0 until count).minByOrNull { k -> abs(centerOf(k) - displayCenter) } ?: 0
                        onSelectedChange(nearest)
                        snapJob = scope.launch {
                            animate(displayCenter, centerOf(nearest), tween(420, easing = FastOutSlowInEasing)) { v, _ ->
                                displayCenter = v
                            }
                        }
                        onDragParallax?.invoke(0f)
                    },
                    onDrag = { change, _ ->
                        val dx = change.position.x - startPointerX
                        if (abs(dx) > 6f) dragged = true
                        val dxDp = with(density) { dx.toDp().value }
                        displayCenter = (startCenter + dxDp)
                            .coerceIn(indicatorWidthDp / 2f, navbarWidthDp - indicatorWidthDp / 2f)
                        onDragParallax?.invoke(dx)
                    },
                )
            },
    ) {
        // 跟随指示器的镜面高光（模拟 liquid glass 的光泽）
        val specXpx = with(density) { displayCenter.dp.toPx() }
        val navWpx = with(density) { navbarWidthDp.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.5f }
                .background(
                    Brush.radialGradient(
                        radius = 110f,
                        center = Offset(specXpx.coerceIn(0f, navWpx), 0f),
                        colors = listOf(Color.White.copy(0.5f), Color.Transparent),
                    ),
                ),
        )

        // 图标 + 文字
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                val isSel = i == selectedIndex
                Column(
                    Modifier
                        .clickable { onSelectedChange(i) }
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
                .clip(RoundedCornerShape((indicatorWidthDp / 2f).dp))
                .background(Color.White.copy(0.20f))
                .border(1.dp, Color.White.copy(0.5f), RoundedCornerShape((indicatorWidthDp / 2f).dp)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(0.5f),
                            0.5f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

/** 导航项数据类（图标 + 文字）。放在你 App 的任意包下都行。 */
data class NavItem(
    val icon: ImageVector,
    val label: String,
)
