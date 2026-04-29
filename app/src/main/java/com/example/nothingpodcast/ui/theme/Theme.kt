package com.example.nothingpodcast.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Nothing Tech dark color scheme.
 *
 * Nothing X is dark-first (AMOLED black). The scheme uses pure black for
 * backgrounds and white as the sole accent, with gray dividers for hierarchy.
 * Rounded corners are intentionally set to 0 via Shapes — Nothing uses sharp edges.
 */
private val NothingDarkColorScheme = darkColorScheme(
    // Backgrounds
    background          = NothingBlack,
    surface             = NothingSurface,
    surfaceVariant      = NothingSurfaceHigh,
    surfaceContainer    = NothingSurfaceMid,
    surfaceContainerHigh = NothingSurfaceHigh,

    // Primary (selected state, CTAs)
    primary             = NothingWhite,
    onPrimary           = NothingBlack,
    primaryContainer    = NothingSurfaceHigh,
    onPrimaryContainer  = NothingWhite,

    // Secondary (supporting UI)
    secondary           = NothingOnSurfaceVariant,
    onSecondary         = NothingBlack,
    secondaryContainer  = NothingSurfaceHigh,
    onSecondaryContainer = NothingWhite,

    // Tertiary (unused — keep neutral)
    tertiary            = NothingOnSurfaceDim,
    onTertiary          = NothingBlack,

    // Text on backgrounds
    onBackground        = NothingOnBackground,
    onSurface           = NothingOnSurface,
    onSurfaceVariant    = NothingOnSurfaceVariant,

    // Outline (borders, dividers)
    outline             = NothingBorder,
    outlineVariant      = NothingBorderDim,

    // Error
    error               = NothingError,
    onError             = NothingOnError,
    errorContainer      = NothingError,
    onErrorContainer    = NothingWhite,
)

/**
 * Light scheme — minimal support, keeps the Nothing monochrome feel
 * but inverted (white background). Not primary target but avoids crashes.
 */
private val NothingLightColorScheme = lightColorScheme(
    background          = NothingWhite,
    surface             = NothingLightSurface,
    primary             = NothingBlack,
    onPrimary           = NothingWhite,
    onBackground        = NothingBlack,
    onSurface           = NothingBlack,
    outline             = NothingBorder,
)

@Composable
fun NothingPodcastTheme(
    // Nothing X is always dark — honour system setting but default to dark
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NothingDarkColorScheme else NothingLightColorScheme

    // Make status bar transparent and icons white (edge-to-edge)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NothingBlack.toArgb()
            window.navigationBarColor = NothingBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NothingTypography,
        shapes      = NothingShapes,
        content     = content
    )
}