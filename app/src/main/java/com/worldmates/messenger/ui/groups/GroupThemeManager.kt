package com.worldmates.messenger.ui.groups

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.worldmates.messenger.ui.preferences.BubbleStyle
import com.worldmates.messenger.ui.theme.PresetBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер кастомних тем для групових чатів.
 *
 * Зберігає кастомізацію кожної групи локально в SharedPreferences.
 * TODO: Додати синхронізацію з сервером коли буде готове API:
 *   POST /api/v2/endpoints/group_customization.php
 *   - save: { group_id, bubble_style, preset_background, accent_color }
 *   - load: { group_id } -> GroupTheme
 *   Потрібна таблиця Wo_GroupCustomization або нові колонки в Wo_GroupsChat
 */
object GroupThemeManager {
    private const val PREFS_NAME = "group_themes"
    private const val KEY_THEMES = "themes_map"

    private val gson = Gson()
    private var prefs: SharedPreferences? = null

    private val _themes = MutableStateFlow<Map<Long, GroupTheme>>(emptyMap())
    val themes: StateFlow<Map<Long, GroupTheme>> = _themes.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadThemes()
    }

    private fun loadThemes() {
        val json = prefs?.getString(KEY_THEMES, null)
        if (json != null) {
            val type = object : TypeToken<Map<Long, GroupTheme>>() {}.type
            _themes.value = gson.fromJson(json, type) ?: emptyMap()
        }
    }

    fun getGroupTheme(groupId: Long): GroupTheme? {
        return _themes.value[groupId]
    }

    fun setGroupTheme(groupId: Long, theme: GroupTheme) {
        val updated = _themes.value.toMutableMap()
        updated[groupId] = theme
        _themes.value = updated
        saveThemes()
    }

    fun removeGroupTheme(groupId: Long) {
        val updated = _themes.value.toMutableMap()
        updated.remove(groupId)
        _themes.value = updated
        saveThemes()
    }

    fun hasCustomTheme(groupId: Long): Boolean {
        return _themes.value.containsKey(groupId)
    }

    private fun saveThemes() {
        prefs?.edit()?.putString(KEY_THEMES, gson.toJson(_themes.value))?.apply()
    }
}

/**
 * Кастомна тема для групового чату.
 * Використовує тільки підключені до рендерингу налаштування.
 */
data class GroupTheme(
    val bubbleStyle: String = BubbleStyle.STANDARD.name,
    val presetBackgroundId: String = PresetBackground.OCEAN.id,
    val accentColor: String = "#2196F3",
    val enabledByAdmin: Boolean = true
) {
    fun getBubbleStyle(): BubbleStyle {
        return try {
            BubbleStyle.valueOf(bubbleStyle)
        } catch (e: Exception) {
            BubbleStyle.STANDARD
        }
    }

    fun getPresetBackground(): PresetBackground {
        return PresetBackground.values().find { it.id == presetBackgroundId }
            ?: PresetBackground.OCEAN
    }

    companion object {
        /** Готові шаблони для швидкого вибору */
        val PRESETS = listOf(
            GroupThemePreset(
                name = "Стандартна",
                emoji = "💬",
                theme = GroupTheme()
            ),
            GroupThemePreset(
                name = "Бізнес",
                emoji = "💼",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.MINIMAL.name,
                    presetBackgroundId = PresetBackground.LAVENDER.id,
                    accentColor = "#607D8B"
                )
            ),
            GroupThemePreset(
                name = "Неон",
                emoji = "⚡",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.NEON.name,
                    presetBackgroundId = PresetBackground.COSMIC.id,
                    accentColor = "#E040FB"
                )
            ),
            GroupThemePreset(
                name = "Природа",
                emoji = "🌿",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.STANDARD.name,
                    presetBackgroundId = PresetBackground.FOREST.id,
                    accentColor = "#4CAF50"
                )
            ),
            GroupThemePreset(
                name = "Романтика",
                emoji = "🌹",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.MODERN.name,
                    presetBackgroundId = PresetBackground.COTTON_CANDY.id,
                    accentColor = "#E91E63"
                )
            ),
            GroupThemePreset(
                name = "Ретро",
                emoji = "📼",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.RETRO.name,
                    presetBackgroundId = PresetBackground.FIRE.id,
                    accentColor = "#FF5722"
                )
            ),
            GroupThemePreset(
                name = "Зима",
                emoji = "❄️",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.NEUMORPHISM.name,
                    presetBackgroundId = PresetBackground.WINTER.id,
                    accentColor = "#00BCD4"
                )
            ),
            GroupThemePreset(
                name = "Космос",
                emoji = "🚀",
                theme = GroupTheme(
                    bubbleStyle = BubbleStyle.GLASS.name,
                    presetBackgroundId = PresetBackground.DEEP_SPACE.id,
                    accentColor = "#7C4DFF"
                )
            )
        )
    }
}

/**
 * Готовий шаблон теми для групи
 */
data class GroupThemePreset(
    val name: String,
    val emoji: String,
    val theme: GroupTheme
)
