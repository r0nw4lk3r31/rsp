package com.sporen.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SporenColorScheme = lightColorScheme(
    primary              = SporenTeal,
    onPrimary            = SporenOnPrimary,
    primaryContainer     = SporenTealDark,
    onPrimaryContainer   = SporenOnPrimary,
    secondary            = SporenOrange,
    onSecondary          = SporenOnPrimary,
    background           = SporenBackground,
    surface              = SporenSurface,
    surfaceVariant       = SporenSurface,
    onBackground         = SporenOnSurface,
    onSurface            = SporenOnSurface,
    onSurfaceVariant     = SporenOnSurfaceMuted,
    error                = Color(0xFFD32F2F),
    onError              = SporenOnPrimary,
)

@Composable
fun SporenRoosterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SporenColorScheme,
        typography = SporenTypography,
        content = content
    )
}

