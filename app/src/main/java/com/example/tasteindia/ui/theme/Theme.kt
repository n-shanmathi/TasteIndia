package com.example.tasteindia.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DeepForestGreen,
    onPrimary = LightSurface,
    primaryContainer = DeepForestGreenLight,
    onPrimaryContainer = LightSurface,
    secondary = SaffronAccent,
    onSecondary = DarkCharcoal,
    secondaryContainer = SoftSaffronBackground,
    onSecondaryContainer = DarkCharcoal,
    tertiary = SoftTerracotta,
    onTertiary = LightSurface,
    background = WarmIvory,
    onBackground = DarkCharcoal,
    surface = LightSurface,
    onSurface = DarkCharcoal,
    surfaceVariant = SoftSaffronBackground,
    onSurfaceVariant = WarmGray,
    outline = CardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DeepForestGreenLight,
    onPrimary = LightSurface,
    primaryContainer = DeepForestGreen,
    onPrimaryContainer = LightSurface,
    secondary = SaffronLight,
    onSecondary = DarkCharcoal,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = SoftTerracotta,
    onTertiary = LightSurface,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurface,
    outline = DarkSurfaceVariant
)

@Composable
fun TasteIndiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
