package com.careshield.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// KernelSU 风格青绿色
val Primary = Color(0xFF2EAF7C)
val PrimaryDark = Color(0xFF4AD99F)
val OnPrimary = Color(0xFFFFFFFF)

val BgLight = Color(0xFFEEF2F1)
val BgDark = Color(0xFF050706)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1A201E)
val TextPrimaryLight = Color(0xFF1A2A26)
val TextSecondaryLight = Color(0xFF71837C)
val TextPrimaryDark = Color(0xFFE8EFEB)
val TextSecondaryDark = Color(0xFF97A6A0)

val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE4F6EC),
    onSurfaceVariant = TextSecondaryLight,
)

val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF06281C),
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF14221D),
    onSurfaceVariant = TextSecondaryDark,
)

/** 主题：0=system 1=light 2=dark，返回实际用的暗色标志 */
@Composable
fun CareShieldTheme(theme: Int, content: @Composable () -> Unit) {
    val dark = when (theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
