package com.worldmates.messenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WMPrimary,
    onPrimary = Color.White,
    primaryContainer = WMPrimaryDark,
    onPrimaryContainer = Color.White,

    secondary = WMSecondary,
    onSecondary = Color.Black,
    secondaryContainer = WMSecondaryDark,
    onSecondaryContainer = Color.White,

    tertiary = WMSecondary,
    onTertiary = Color.Black,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = TextSecondaryDark,

    error = Error,
    onError = Color.White,

    outline = DividerDark,
    outlineVariant = DividerDark.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = WMPrimary,
    onPrimary = Color.White,
    primaryContainer = WMPrimaryLight,
    onPrimaryContainer = Color.Black,

    secondary = WMSecondary,
    onSecondary = Color.Black,
    secondaryContainer = WMSecondaryLight,
    onSecondaryContainer = Color.Black,

    tertiary = WMSecondary,
    onTertiary = Color.Black,

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,

    error = Error,
    onError = Color.White,

    outline = Divider,
    outlineVariant = Divider.copy(alpha = 0.5f)
)

/**
 * Главная тема WorldMates Messenger
 *
 * @param darkTheme использовать темную тему (по умолчанию следует системной настройке)
 * @param content контент приложения
 */
@Composable
fun WorldMatesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WMTypography,
        shapes = Shapes,
        content = content
    )
}
