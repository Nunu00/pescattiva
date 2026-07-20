package com.vincenzo.previsionipesca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    secondary = GoldAccent,
    background = MidnightBlue,
    surface = DarkNavy,
    onPrimary = MidnightBlue,
    onSecondary = MidnightBlue,
    onBackground = LightWhite,
    onSurface = LightWhite
)

@Composable
fun PrevisioniPescaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
