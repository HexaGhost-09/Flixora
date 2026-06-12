package com.hexaghost.flixora.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FlixoraDarkColorScheme = darkColorScheme(
    primary = FlixoraPurple,
    onPrimary = FlixoraWhite,
    primaryContainer = FlixoraDeepPurple,
    onPrimaryContainer = FlixoraWhite,
    secondary = FlixoraCyan,
    onSecondary = FlixoraDarkBg,
    secondaryContainer = Color(0xFF003A4D),
    onSecondaryContainer = FlixoraCyan,
    tertiary = FlixoraGold,
    onTertiary = FlixoraDarkBg,
    background = FlixoraDarkBg,
    onBackground = FlixoraWhite,
    surface = FlixoraDarkSurface,
    onSurface = FlixoraWhite,
    surfaceVariant = FlixoraDarkCard,
    onSurfaceVariant = FlixoraWhite80,
    surfaceTint = FlixoraPurple,
    error = ErrorRed,
    onError = FlixoraWhite,
    outline = FlixoraWhite40,
    outlineVariant = FlixoraWhite40,
    scrim = Color(0x99000000),
    inverseSurface = FlixoraWhite,
    inverseOnSurface = FlixoraDarkBg,
    inversePrimary = FlixoraDeepPurple
)

@Composable
fun FlixoraTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = FlixoraDarkBg.toArgb()
            window.navigationBarColor = FlixoraDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = FlixoraDarkColorScheme,
        typography = FlixoraTypography,
        content = content
    )
}
