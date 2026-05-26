package com.example.myapplication.ui.theme

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

// ─── Always force our custom dark scheme — NEVER use dynamic wallpaper colors ──
private val CanaraAIDarkScheme = darkColorScheme(
    primary                  = primaryDark,
    onPrimary                = onPrimaryDark,
    primaryContainer         = primaryContainerDark,
    onPrimaryContainer       = onPrimaryContainerDark,
    secondary                = secondaryDark,
    onSecondary              = onSecondaryDark,
    secondaryContainer       = secondaryContainerDark,
    onSecondaryContainer     = onSecondaryContainerDark,
    tertiary                 = tertiaryDark,
    onTertiary               = onTertiaryDark,
    tertiaryContainer        = tertiaryContainerDark,
    onTertiaryContainer      = onTertiaryContainerDark,
    error                    = errorDark,
    onError                  = onErrorDark,
    errorContainer           = errorContainerDark,
    onErrorContainer         = onErrorContainerDark,
    background               = backgroundDark,
    onBackground             = onBackgroundDark,
    surface                  = surfaceDark2,
    onSurface                = onSurfaceDark,
    surfaceVariant           = surfaceVariantDark,
    onSurfaceVariant         = onSurfaceVariantDark,
    outline                  = outlineDark,
    outlineVariant           = outlineVariantDark,
    scrim                    = scrimDark,
    inverseSurface           = inverseSurfaceDark,
    inverseOnSurface         = inverseOnSurfaceDark,
    inversePrimary           = inversePrimaryDark,
)

private val CanaraAILightScheme = lightColorScheme(
    primary                  = primaryLight,
    onPrimary                = onPrimaryLight,
    primaryContainer         = primaryContainerLight,
    onPrimaryContainer       = onPrimaryContainerLight,
    secondary                = secondaryLight,
    onSecondary              = onSecondaryLight,
    secondaryContainer       = secondaryContainerLight,
    onSecondaryContainer     = onSecondaryContainerLight,
    tertiary                 = tertiaryLight,
    onTertiary               = onTertiaryLight,
    tertiaryContainer        = tertiaryContainerLight,
    onTertiaryContainer      = onTertiaryContainerLight,
    error                    = errorLight,
    onError                  = onErrorLight,
    errorContainer           = errorContainerLight,
    onErrorContainer         = onErrorContainerLight,
    background               = backgroundLight,
    onBackground             = onBackgroundLight,
    surface                  = surfaceLight2,
    onSurface                = onSurfaceLight,
    surfaceVariant           = surfaceVariantLight,
    onSurfaceVariant         = onSurfaceVariantLight,
    outline                  = outlineLight,
    outlineVariant           = outlineVariantLight,
    scrim                    = scrimLight,
    inverseSurface           = inverseSurfaceLight,
    inverseOnSurface         = inverseOnSurfaceLight,
    inversePrimary           = inversePrimaryLight,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // ALWAYS dark — equal AI style
    content: @Composable () -> Unit
) {
    // dynamicColor is DISABLED — we enforce our custom Canara AI brand palette
    val colorScheme = if (darkTheme) CanaraAIDarkScheme else CanaraAILightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
