package com.example.ocrmsg.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = BackgroundDark,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    secondary        = AccentCyanMid,
    onSecondary      = BackgroundDark,
)

@Composable
fun OCRmsgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}