package com.worldmates.messenger.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Варианты тем WorldMates Messenger
 */
enum class ThemeVariant(
    val displayName: String,
    val description: String,
    val iconResId: Int? = null
) {
    CLASSIC(
        displayName = "Классическая",
        description = "Официальная тема WorldMates",
        iconResId = null
    ),
    MATERIAL_YOU(
        displayName = "Material You",
        description = "Динамические цвета из обоев",
        iconResId = null
    ),
    NIGHT_OCEAN(
        displayName = "Ночной океан",
        description = "Глубокие синие тона",
        iconResId = null
    ),
    SUNSET_GLOW(
        displayName = "Закатное сияние",
        description = "Теплые оранжевые градиенты",
        iconResId = null
    ),
    FOREST_DEEP(
        displayName = "Глубокий лес",
        description = "Натуральные зеленые оттенки",
        iconResId = null
    ),
    NEON_DREAM(
        displayName = "Неоновый сон",
        description = "Яркие футуристичные цвета",
        iconResId = null
    ),
    MONOCHROME(
        displayName = "Монохром",
        description = "Черно-белая элегантность",
        iconResId = null
    ),
    RETRO_80S(
        displayName = "Ретро 80-е",
        description = "Яркие ретро-цвета",
        iconResId = null
    ),
    DARK_MATTER(
        displayName = "Темная материя",
        description = "Глубокий космический черный",
        iconResId = null
    ),
    AURORA(
        displayName = "Северное сияние",
        description = "Полярные градиенты",
        iconResId = null
    );

    /**
     * Получить палитру для этого варианта темы
     */
    fun getPalette(): ThemePalette {
        return when (this) {
            CLASSIC -> ClassicPalette
            MATERIAL_YOU -> MaterialYouPalette
            NIGHT_OCEAN -> NightOceanPalette
            SUNSET_GLOW -> SunsetGlowPalette
            FOREST_DEEP -> ForestDeepPalette
            NEON_DREAM -> NeonDreamPalette
            MONOCHROME -> MonochromePalette
            RETRO_80S -> Retro80sPalette
            DARK_MATTER -> DarkMatterPalette
            AURORA -> AuroraPalette
        }
    }

    companion object {
        /**
         * Получить список всех вариантов тем
         */
        fun getAllVariants(): List<ThemeVariant> {
            return values().toList()
        }

        /**
         * Получить вариант темы по имени
         */
        fun fromName(name: String): ThemeVariant {
            return values().find { it.name == name } ?: CLASSIC
        }
    }
}

/**
 * Палитра цветов для варианта темы
 */
data class ThemePalette(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val accent: Color,
    val messageBubbleOwn: Color,
    val messageBubbleOther: Color,
    val backgroundGradient: Brush
)

// ==================== РЕАЛЬНЫЕ ПАЛИТРЫ ====================

// Классическая палитра
val ClassicPalette = ThemePalette(
    primary = Color(0xFF0A84FF),
    primaryDark = Color(0xFF0040DD),
    primaryLight = Color(0xFF5AC8FA),
    secondary = Color(0xFF00D4FF),
    secondaryDark = Color(0xFF00A0C8),
    secondaryLight = Color(0xFF64D2FF),
    accent = Color(0xFFFF6B9D),
    messageBubbleOwn = Color(0xFF0A84FF),
    messageBubbleOther = Color(0xFFF0F2F5),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0A84FF), Color(0xFF00D4FF), Color(0xFF5AC8FA))
    )
)

// Material You палитра
val MaterialYouPalette = ThemePalette(
    primary = Color(0xFF6750A4),
    primaryDark = Color(0xFF4F378B),
    primaryLight = Color(0xFFEADDFF),
    secondary = Color(0xFF625B71),
    secondaryDark = Color(0xFF4A4458),
    secondaryLight = Color(0xFFE8DEF8),
    accent = Color(0xFF7D5260),
    messageBubbleOwn = Color(0xFF6750A4),
    messageBubbleOther = Color(0xFFE6E1E5),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFFE8DEF8))
    )
)

// Ночной океан
val NightOceanPalette = ThemePalette(
    primary = Color(0xFF0066CC),
    primaryDark = Color(0xFF004C99),
    primaryLight = Color(0xFF3399FF),
    secondary = Color(0xFF00CCFF),
    secondaryDark = Color(0xFF0099CC),
    secondaryLight = Color(0xFF66E0FF),
    accent = Color(0xFF00FFCC),
    messageBubbleOwn = Color(0xFF0066CC),
    messageBubbleOther = Color(0xFF1A1F2C),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF001122), Color(0xFF003366), Color(0xFF0066CC))
    )
)

// Закатное сияние
val SunsetGlowPalette = ThemePalette(
    primary = Color(0xFFFF6B35),
    primaryDark = Color(0xFFCC552A),
    primaryLight = Color(0xFFFFA07A),
    secondary = Color(0xFFFFD166),
    secondaryDark = Color(0xFFCCAA52),
    secondaryLight = Color(0xFFFFE4A6),
    accent = Color(0xFFEF476F),
    messageBubbleOwn = Color(0xFFFF6B35),
    messageBubbleOther = Color(0xFFFFF5E6),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF6B35), Color(0xFFFFD166), Color(0xFFEF476F))
    )
)

// Глубокий лес
val ForestDeepPalette = ThemePalette(
    primary = Color(0xFF2E8B57),
    primaryDark = Color(0xFF1E6B47),
    primaryLight = Color(0xFF5CDB95),
    secondary = Color(0xFF8FBC8F),
    secondaryDark = Color(0xFF6F9E6F),
    secondaryLight = Color(0xFFC1FFC1),
    accent = Color(0xFFFFD700),
    messageBubbleOwn = Color(0xFF2E8B57),
    messageBubbleOther = Color(0xFFF0FFF0),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1B4332), Color(0xFF2E8B57), Color(0xFF5CDB95))
    )
)

// Неоновый сон
val NeonDreamPalette = ThemePalette(
    primary = Color(0xFF00FF88),
    primaryDark = Color(0xFF00CC6C),
    primaryLight = Color(0xFF66FFB3),
    secondary = Color(0xFFBF00FF),
    secondaryDark = Color(0xFF9900CC),
    secondaryLight = Color(0xFFE066FF),
    accent = Color(0xFFFF006E),
    messageBubbleOwn = Color(0xFF00FF88),
    messageBubbleOther = Color(0xFF1A1A1A),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF00FF88), Color(0xFFBF00FF))
    )
)

// Монохром
val MonochromePalette = ThemePalette(
    primary = Color(0xFF333333),
    primaryDark = Color(0xFF000000),
    primaryLight = Color(0xFF666666),
    secondary = Color(0xFF999999),
    secondaryDark = Color(0xFF666666),
    secondaryLight = Color(0xFFCCCCCC),
    accent = Color(0xFFFFFFFF),
    messageBubbleOwn = Color(0xFF333333),
    messageBubbleOther = Color(0xFFF5F5F5),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF333333), Color(0xFF666666))
    )
)

// Ретро 80-е
val Retro80sPalette = ThemePalette(
    primary = Color(0xFFFF00FF),
    primaryDark = Color(0xFFCC00CC),
    primaryLight = Color(0xFFFF66FF),
    secondary = Color(0xFF00FFFF),
    secondaryDark = Color(0xFF00CCCC),
    secondaryLight = Color(0xFF66FFFF),
    accent = Color(0xFFFFFF00),
    messageBubbleOwn = Color(0xFFFF00FF),
    messageBubbleOther = Color(0xFF000000),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF00FF), Color(0xFF00FFFF), Color(0xFFFFFF00))
    )
)

// Темная материя
val DarkMatterPalette = ThemePalette(
    primary = Color(0xFF121212),
    primaryDark = Color(0xFF000000),
    primaryLight = Color(0xFF333333),
    secondary = Color(0xFFBB86FC),
    secondaryDark = Color(0xFF3700B3),
    secondaryLight = Color(0xFFE1BEE7),
    accent = Color(0xFF03DAC6),
    messageBubbleOwn = Color(0xFFBB86FC),
    messageBubbleOther = Color(0xFF1E1E1E),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF000000), Color(0xFF121212), Color(0xFF1E1E1E))
    )
)

// Северное сияние
val AuroraPalette = ThemePalette(
    primary = Color(0xFF00C9FF),
    primaryDark = Color(0xFF009ECC),
    primaryLight = Color(0xFF66E0FF),
    secondary = Color(0xFF92FE9D),
    secondaryDark = Color(0xFF74CB7D),
    secondaryLight = Color(0xFFC8FFCD),
    accent = Color(0xFFFF00FF),
    messageBubbleOwn = Color(0xFF00C9FF),
    messageBubbleOther = Color(0xFF0A0F1C),
    backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0A0F1C), Color(0xFF00C9FF), Color(0xFF92FE9D))
    )
)