package com.careshield.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 界面风格切换：COUI（ColorOS 风）/ Material3（原 KernelSU 绿风格）。
 *
 * COUI 设计特征（ColorOS 7+ 设计系统共识）：
 *  - 大圆角卡片（20~28dp）
 *  - 大量留白、浅色模式底色极浅灰，深色模式深灰（非纯黑）
 *  - 主色 OPPO 品牌绿系，强调清爽
 *  - 胶囊(Capsule)按钮、大标题
 */
enum class UiStyle(val key: String, val label: String) {
    COUI("coui", "COUI"),
    MATERIAL3("material3", "Material3");

    companion object {
        fun from(key: String): UiStyle =
            entries.firstOrNull { it.key == key } ?: COUI
    }
}

/** 当前风格（组合内可读，跨页面共享） */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.COUI }

// ================= COUI 色板 =================
object CouiColors {
    val GreenLight = Color(0xFF26C281)   // ColorOS 品牌绿（浅色模式）
    val GreenDark = Color(0xFF4BE3A4)    // 深色模式下更亮的绿

    // 浅色：近白底、白卡、黑字
    val BgLight = Color(0xFFF6F7F8)
    val CardLight = Color(0xFFFFFFFF)
    val TextPrimaryLight = Color(0xFF1A1A1A)
    val TextSecondaryLight = Color(0xFF8A9094)
    val SurfaceVariantLight = Color(0xFFEFF1F2)

    // 深色：深灰底、深灰卡、白字（ColorOS 深色模式不是纯黑）
    val BgDark = Color(0xFF111212)
    val CardDark = Color(0xFF1C1E1F)
    val TextPrimaryDark = Color(0xFFEDEFEF)
    val TextSecondaryDark = Color(0xFF868C8F)
    val SurfaceVariantDark = Color(0xFF232526)
}

val CouiLightColors = lightColorScheme(
    primary = CouiColors.GreenLight,
    onPrimary = Color.White,
    background = CouiColors.BgLight,
    onBackground = CouiColors.TextPrimaryLight,
    surface = CouiColors.CardLight,
    onSurface = CouiColors.TextPrimaryLight,
    surfaceVariant = CouiColors.SurfaceVariantLight,
    onSurfaceVariant = CouiColors.TextSecondaryLight,
    secondaryContainer = CouiColors.SurfaceVariantLight,
    onSecondaryContainer = CouiColors.TextPrimaryLight,
)

val CouiDarkColors = darkColorScheme(
    primary = CouiColors.GreenDark,
    onPrimary = Color(0xFF00391F),
    background = CouiColors.BgDark,
    onBackground = CouiColors.TextPrimaryDark,
    surface = CouiColors.CardDark,
    onSurface = CouiColors.TextPrimaryDark,
    surfaceVariant = CouiColors.SurfaceVariantDark,
    onSurfaceVariant = CouiColors.TextSecondaryDark,
    secondaryContainer = CouiColors.SurfaceVariantDark,
    onSecondaryContainer = CouiColors.TextPrimaryDark,
)

// ================= 原风格色板（KernelSU 绿，Material3）=================
val Primary = Color(0xFF2EAF7C)
val PrimaryDark = Color(0xFF4AD99F)
val BgLight = Color(0xFFEEF2F1)
val BgDark = Color(0xFF050706)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1A201E)
val TextPrimaryLight = Color(0xFF1A2A26)
val TextSecondaryLight = Color(0xFF71837C)
val TextPrimaryDark = Color(0xFFE8EFEB)
val TextSecondaryDark = Color(0xFF97A6A0)

val M3LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE4F6EC),
    onSurfaceVariant = TextSecondaryLight,
)

val M3DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF06281C),
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF14221D),
    onSurfaceVariant = TextSecondaryDark,
)

// ================= 形状 =================
val CouiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val M3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * 双风格主题入口。
 * @param theme 0=跟随系统 1=浅色 2=深色
 * @param style UiStyle.COUI / UiStyle.MATERIAL3
 */
@Composable
fun CareShieldTheme(theme: Int, style: UiStyle = UiStyle.COUI, content: @Composable () -> Unit) {
    val dark = when (theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when (style) {
        UiStyle.COUI -> if (dark) CouiDarkColors else CouiLightColors
        UiStyle.MATERIAL3 -> if (dark) M3DarkColors else M3LightColors
    }
    val shapes = when (style) {
        UiStyle.COUI -> CouiShapes
        UiStyle.MATERIAL3 -> M3Shapes
    }
    CompositionLocalProvider(LocalUiStyle provides style) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            content = content
        )
    }
}

/** 组合内便捷读取 */
@Composable
fun currentUiStyle(): UiStyle = LocalUiStyle.current
