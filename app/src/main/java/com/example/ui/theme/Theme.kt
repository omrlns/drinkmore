package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = HydrationPrimaryDark,
    secondary = HydrationSecondaryDark,
    tertiary = HydrationTertiaryDark,
    background = HydrationBgDark,
    surface = HydrationSurfaceDark,
    onPrimary = HydrationOnPrimaryDark,
    onSecondary = HydrationOnPrimaryDark,
    onBackground = HydrationTextDark,
    onSurface = HydrationTextDark,
    surfaceVariant = HydrationSurfaceDark,
    onSurfaceVariant = HydrationTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = HydrationPrimaryLight,
    secondary = HydrationSecondaryLight,
    tertiary = HydrationTertiaryLight,
    background = HydrationBgLight,
    surface = HydrationSurfaceLight,
    onPrimary = HydrationOnPrimaryLight,
    onSecondary = HydrationOnPrimaryLight,
    onBackground = HydrationTextLight,
    onSurface = HydrationTextLight,
    surfaceVariant = HydrationSurfaceLight,
    onSurfaceVariant = HydrationTextLight
)

@Composable
fun MyApplicationTheme(
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
