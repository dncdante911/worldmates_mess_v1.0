package com.worldmates.messenger.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Дополнительные цвета и эффекты WorldMates
 * Включает динамические фоны для каждой темы
 */
@Immutable
data class ExtendedColors(
    val messageBubbleOwn: Color,
    val messageBubbleOther: Color,
    val messageBubbleOwnDark: Color,
    val messageBubbleOtherDark: Color,
    val onlineGreen: Color,
    val awayYellow: Color,
    val busyRed: Color,
    val offlineGray: Color,
    val unreadBadge: Color,
    val typingIndicator: Color,
    val searchBarBackground: Color,
    val backgroundGradient: Brush  // Динамический градиентный фон
)

/**
 * CompositionLocal для расширенных цветов и эффектов
 */
val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        messageBubbleOwn = Color.Unspecified,
        messageBubbleOther = Color.Unspecified,
        messageBubbleOwnDark = Color.Unspecified,
        messageBubbleOtherDark = Color.Unspecified,
        onlineGreen = Color.Unspecified,
        awayYellow = Color.Unspecified,
        busyRed = Color.Unspecified,
        offlineGray = Color.Unspecified,
        unreadBadge = Color.Unspecified,
        typingIndicator = Color.Unspecified,
        searchBarBackground = Color.Unspecified,
        backgroundGradient = Brush.linearGradient(listOf(Color.Black, Color.Black))
    )
}

/**
 * Получить расширенные цвета для текущей темы
 */
object WMColors {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

/**
 * Создать светлую цветовую схему для заданного варианта темы (Material 3)
 */
private fun createLightColorScheme(palette: ThemePalette): ColorScheme {
    return lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryLight,
        onPrimaryContainer = Color.Black,

        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryLight,
        onSecondaryContainer = Color.Black,

        tertiary = palette.accent,
        onTertiary = Color.White,

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
}

/**
 * Создать темную цветовую схему для заданного варианта темы (Material 3)
 */
private fun createDarkColorScheme(palette: ThemePalette): ColorScheme {
    return darkColorScheme(
        primary = palette.primaryLight,
        onPrimary = Color.Black,
        primaryContainer = palette.primaryDark,
        onPrimaryContainer = Color.White,

        secondary = palette.secondaryLight,
        onSecondary = Color.Black,
        secondaryContainer = palette.secondaryDark,
        onSecondaryContainer = Color.White,

        tertiary = palette.accent,
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
}

/**
 * Создать расширенные цвета и эффекты для заданного варианта темы
 */
private fun createExtendedColors(
    palette: ThemePalette,
    isDark: Boolean
): ExtendedColors {
    return ExtendedColors(
        messageBubbleOwn = palette.messageBubbleOwn,
        messageBubbleOther = palette.messageBubbleOther,
        messageBubbleOwnDark = palette.primaryDark,
        messageBubbleOtherDark = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5E5EA),
        onlineGreen = OnlineGreen,
        awayYellow = AwayYellow,
        busyRed = BusyRed,
        offlineGray = OfflineGray,
        unreadBadge = UnreadBadge,
        typingIndicator = palette.primary,
        searchBarBackground = if (isDark) SearchBarBackgroundDark else SearchBarBackground,
        backgroundGradient = palette.backgroundGradient
    )
}

/**
 * Настройка системных баров для Material 3
 */
@Composable
private fun SetupSystemBars(
    window: Window?,
    colorScheme: ColorScheme,
    isDarkTheme: Boolean
) {
    SideEffect {
        window?.let {
            it.statusBarColor = colorScheme.surface.toArgb()
            it.navigationBarColor = colorScheme.surface.toArgb()

            WindowCompat.getInsetsController(it, it.decorView).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }
}

/**
 * Главная тема WorldMates Messenger с поддержкой Material 3
 */
@Composable
fun WorldMatesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeVariant: ThemeVariant = ThemeVariant.CLASSIC,
    dynamicColor: Boolean = false,
    animateColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    // Обновляем состояние темы
    ThemeState.currentVariant = themeVariant
    ThemeState.isDarkTheme = darkTheme
    ThemeState.useDynamicColors = dynamicColor

    // Определяем цветовую схему Material 3
    val colorScheme = when {
        // Material You (динамические цвета) - Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // Кастомные темы
        else -> {
            val palette = themeVariant.getPalette()
            if (darkTheme) {
                createDarkColorScheme(palette)
            } else {
                createLightColorScheme(palette)
            }
        }
    }

    // Создаем расширенные цвета
    val extendedColors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val materialYouPalette = ThemeVariant.MATERIAL_YOU.getPalette()
            ExtendedColors(
                messageBubbleOwn = colorScheme.primary,
                messageBubbleOther = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5EA),
                messageBubbleOwnDark = colorScheme.primaryContainer,
                messageBubbleOtherDark = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFE5E5EA),
                onlineGreen = OnlineGreen,
                awayYellow = AwayYellow,
                busyRed = BusyRed,
                offlineGray = OfflineGray,
                unreadBadge = UnreadBadge,
                typingIndicator = colorScheme.primary,
                searchBarBackground = if (darkTheme) SearchBarBackgroundDark else SearchBarBackground,
                backgroundGradient = materialYouPalette.backgroundGradient
            )
        }
        else -> {
            val palette = themeVariant.getPalette()
            createExtendedColors(palette, darkTheme)
        }
    }

    // Настройка системных баров
    SetupSystemBars(window, colorScheme, darkTheme)

    // Анимация перехода между темами
    val animatedContent = @Composable {
        CompositionLocalProvider(
            LocalExtendedColors provides extendedColors
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = WMTypography,
                shapes = Shapes,
                content = content
            )
        }
    }

    if (animateColors) {
        Crossfade(
            targetState = Triple(themeVariant, darkTheme, colorScheme),
            label = "ThemeTransition"
        ) {
            animatedContent()
        }
    } else {
        animatedContent()
    }
}

/**
 * Упрощенная версия темы для быстрого использования
 */
@Composable
fun SimpleWorldMatesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    WorldMatesTheme(
        darkTheme = darkTheme,
        themeVariant = ThemeVariant.CLASSIC,
        dynamicColor = false,
        animateColors = false,
        content = content
    )
}