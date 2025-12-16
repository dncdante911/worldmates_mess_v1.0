package com.worldmates.messenger.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Варианты тем для WorldMates Messenger
 */
enum class ThemeVariant(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    CLASSIC(
        displayName = "Classic Blue",
        emoji = "💙",
        description = "Классическая синяя тема в стиле Messenger"
    ),

    OCEAN(
        displayName = "Deep Ocean",
        emoji = "🌊",
        description = "Глубокие океанские оттенки синего и бирюзового"
    ),

    SUNSET(
        displayName = "Sunset Dreams",
        emoji = "🌅",
        description = "Теплые оранжево-розовые тона заката"
    ),

    FOREST(
        displayName = "Forest Green",
        emoji = "🌲",
        description = "Природные зеленые и изумрудные оттенки"
    ),

    PURPLE(
        displayName = "Purple Dream",
        emoji = "💜",
        description = "Элегантные фиолетовые и сиреневые тона"
    ),

    ROSE_GOLD(
        displayName = "Rose Gold",
        emoji = "🌹",
        description = "Утонченное сочетание розового и золотого"
    ),

    MONOCHROME(
        displayName = "Monochrome",
        emoji = "⚫",
        description = "Минималистичная черно-белая тема"
    ),

    NORD(
        displayName = "Nord Frost",
        emoji = "❄️",
        description = "Холодные северные оттенки Nord палитры"
    ),

    DRACULA(
        displayName = "Dracula Night",
        emoji = "🦇",
        description = "Темная тема с яркими акцентами"
    ),

    MATERIAL_YOU(
        displayName = "Material You",
        emoji = "🎨",
        description = "Динамические цвета из обоев (Android 12+)"
    );

    companion object {
        fun fromOrdinal(ordinal: Int): ThemeVariant {
            return values().getOrNull(ordinal) ?: CLASSIC
        }

        fun fromName(name: String): ThemeVariant {
            return values().find { it.name == name } ?: CLASSIC
        }
    }
}

/**
 * Палитра цветов для каждой темы
 */
data class ThemePalette(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val messageBubbleOwn: Color,
    val messageBubbleOther: Color,
    val accent: Color
)

/**
 * Получить палитру для конкретного варианта темы
 */
fun ThemeVariant.getPalette(): ThemePalette {
    return when (this) {
        ThemeVariant.CLASSIC -> ThemePalette(
            primary = Color(0xFF0A84FF),  // Более яркий iOS-стиль синий
            primaryDark = Color(0xFF0040DD),  // Глубокий электрический синий
            primaryLight = Color(0xFF5AC8FA),  // Светло-голубой
            secondary = Color(0xFF00D4FF),  // Яркий циан
            secondaryDark = Color(0xFF00A0C8),  // Темный циан
            secondaryLight = Color(0xFF64D2FF),  // Светлый циан
            messageBubbleOwn = Color(0xFF0A84FF),  // Синий для своих сообщений
            messageBubbleOther = Color(0xFFE9ECEF),  // Светло-серый для чужих
            accent = Color(0xFF5AC8FA)  // Акцентный голубой
        )

        ThemeVariant.OCEAN -> ThemePalette(
            primary = Color(0xFF006BA6),
            primaryDark = Color(0xFF004E7A),
            primaryLight = Color(0xFF4D9FD1),
            secondary = Color(0xFF00A1B8),
            secondaryDark = Color(0xFF008394),
            secondaryLight = Color(0xFF4DC4D4),
            messageBubbleOwn = Color(0xFF006BA6),
            messageBubbleOther = Color(0xFFE0F2F7),
            accent = Color(0xFF00BCD4)
        )

        ThemeVariant.SUNSET -> ThemePalette(
            primary = Color(0xFFFF6B35),
            primaryDark = Color(0xFFD94E28),
            primaryLight = Color(0xFFFF9671),
            secondary = Color(0xFFFF8E53),
            secondaryDark = Color(0xFFE67542),
            secondaryLight = Color(0xFFFFC09F),
            messageBubbleOwn = Color(0xFFFF6B35),
            messageBubbleOther = Color(0xFFFFF0E6),
            accent = Color(0xFFFFB347)
        )

        ThemeVariant.FOREST -> ThemePalette(
            primary = Color(0xFF2E7D32),
            primaryDark = Color(0xFF1B5E20),
            primaryLight = Color(0xFF66BB6A),
            secondary = Color(0xFF00897B),
            secondaryDark = Color(0xFF00695C),
            secondaryLight = Color(0xFF4DB6AC),
            messageBubbleOwn = Color(0xFF2E7D32),
            messageBubbleOther = Color(0xFFE8F5E9),
            accent = Color(0xFF4CAF50)
        )

        ThemeVariant.PURPLE -> ThemePalette(
            primary = Color(0xFF6A1B9A),
            primaryDark = Color(0xFF4A148C),
            primaryLight = Color(0xFF9C4DCC),
            secondary = Color(0xFF8E24AA),
            secondaryDark = Color(0xFF6A1B9A),
            secondaryLight = Color(0xFFBA68C8),
            messageBubbleOwn = Color(0xFF6A1B9A),
            messageBubbleOther = Color(0xFFF3E5F5),
            accent = Color(0xFF9C27B0)
        )

        ThemeVariant.ROSE_GOLD -> ThemePalette(
            primary = Color(0xFFE91E63),
            primaryDark = Color(0xFFC2185B),
            primaryLight = Color(0xFFF06292),
            secondary = Color(0xFFFFB74D),
            secondaryDark = Color(0xFFFF9800),
            secondaryLight = Color(0xFFFFCC80),
            messageBubbleOwn = Color(0xFFE91E63),
            messageBubbleOther = Color(0xFFFCE4EC),
            accent = Color(0xFFFF4081)
        )

        ThemeVariant.MONOCHROME -> ThemePalette(
            primary = Color(0xFF212121),
            primaryDark = Color(0xFF000000),
            primaryLight = Color(0xFF484848),
            secondary = Color(0xFF616161),
            secondaryDark = Color(0xFF424242),
            secondaryLight = Color(0xFF9E9E9E),
            messageBubbleOwn = Color(0xFF212121),
            messageBubbleOther = Color(0xFFF5F5F5),
            accent = Color(0xFF000000)
        )

        ThemeVariant.NORD -> ThemePalette(
            primary = Color(0xFF5E81AC),
            primaryDark = Color(0xFF4C566A),
            primaryLight = Color(0xFF81A1C1),
            secondary = Color(0xFF88C0D0),
            secondaryDark = Color(0xFF8FBCBB),
            secondaryLight = Color(0xFFD8DEE9),
            messageBubbleOwn = Color(0xFF5E81AC),
            messageBubbleOther = Color(0xFFECEFF4),
            accent = Color(0xFF88C0D0)
        )

        ThemeVariant.DRACULA -> ThemePalette(
            primary = Color(0xFFBD93F9),
            primaryDark = Color(0xFF9B6EE8),
            primaryLight = Color(0xFFD4B5FF),
            secondary = Color(0xFFFF79C6),
            secondaryDark = Color(0xFFFF5AC8),
            secondaryLight = Color(0xFFFFB3E5),
            messageBubbleOwn = Color(0xFFBD93F9),
            messageBubbleOther = Color(0xFF44475A),
            accent = Color(0xFF50FA7B)
        )

        ThemeVariant.MATERIAL_YOU -> ThemePalette(
            primary = Color(0xFF6750A4),
            primaryDark = Color(0xFF4F378B),
            primaryLight = Color(0xFF9A82DB),
            secondary = Color(0xFF625B71),
            secondaryDark = Color(0xFF4A4458),
            secondaryLight = Color(0xFF938F99),
            messageBubbleOwn = Color(0xFF6750A4),
            messageBubbleOther = Color(0xFFE8DEF8),
            accent = Color(0xFF6750A4)
        )
    }
}
