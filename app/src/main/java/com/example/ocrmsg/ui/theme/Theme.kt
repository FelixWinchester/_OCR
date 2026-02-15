package com.example.ocrmsg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleDark,
    background = BackgroundLight,
    surface = PurpleLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleDark,
    background = PurpleDark,
    surface = PurplePrimary
)

@Composable
fun OCRmsgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
