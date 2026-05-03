package com.rooster.rooster.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RoosterDarkColorScheme = darkColorScheme(
    primary = RoosterPrimary,
    onPrimary = RoosterOnPrimary,
    primaryContainer = RoosterPrimaryContainer,
    onPrimaryContainer = RoosterOnPrimaryContainer,
    secondary = RoosterSecondary,
    onSecondary = RoosterOnSecondary,
    secondaryContainer = RoosterSecondaryContainer,
    onSecondaryContainer = RoosterOnSecondaryContainer,
    tertiary = RoosterTertiary,
    onTertiary = RoosterOnTertiary,
    background = RoosterBackground,
    onBackground = RoosterOnBackground,
    surface = RoosterSurface,
    onSurface = RoosterOnSurface,
    surfaceVariant = RoosterSurfaceVariant,
    onSurfaceVariant = RoosterOnSurfaceVariant,
    surfaceContainerHigh = RoosterSurfaceContainerHigh,
    outline = RoosterOutline,
)

@Composable
fun RoosterTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = RoosterDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
