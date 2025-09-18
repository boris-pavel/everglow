package com.boris.everglow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GlowPrimaryDark,
    onPrimary = GlowOnDark,
    secondary = GlowSecondary,
    onSecondary = GlowOnDark,
    tertiary = GlowHighlight,
    background = GlowBackdrop,
    surface = GlowSurface,
    surfaceVariant = GlowSurfaceContainer,
    onSurface = GlowOnDark,
    onBackground = GlowOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = GlowPrimary,
    onPrimary = GlowOnDark,
    secondary = GlowSecondary,
    onSecondary = GlowOnDark,
    tertiary = GlowHighlight,
    background = Color(0xFF141F3A),
    surface = GlowSurface,
    surfaceVariant = Color(0xFF172342),
    onSurface = GlowOnDark,
    onBackground = GlowOnDark
)

@Composable
fun EverglowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
