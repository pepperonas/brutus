package com.pepperonas.brutus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BrutusDarkScheme = darkColorScheme(
    primary = BrutusRed,
    onPrimary = BrutusTextPrimary,
    secondary = BrutusOrange,
    onSecondary = BrutusTextPrimary,
    background = BrutusDark,
    onBackground = BrutusTextPrimary,
    surface = BrutusSurface,
    onSurface = BrutusTextPrimary,
    surfaceVariant = BrutusCard,
    onSurfaceVariant = BrutusTextSecondary,
    error = BrutusRed,
)

@Composable
fun BrutusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrutusDarkScheme,
        typography = BrutusTypography,
        content = content
    )
}
