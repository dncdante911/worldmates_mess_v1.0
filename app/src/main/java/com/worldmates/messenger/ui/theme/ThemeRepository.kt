package com.worldmates.messenger.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore для хранения настроек темы
 */
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * Репозиторий для управления настройками темы приложения
 */
class ThemeRepository(private val context: Context) {

    companion object {
        private val THEME_VARIANT_KEY = intPreferencesKey("theme_variant")
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val SYSTEM_THEME_KEY = booleanPreferencesKey("system_theme")
    }

    /**
     * Получить текущий вариант темы
     */
    val themeVariant: Flow<ThemeVariant> = context.themeDataStore.data
        .map { preferences ->
            val ordinal = preferences[THEME_VARIANT_KEY] ?: ThemeVariant.CLASSIC.ordinal
            ThemeVariant.fromOrdinal(ordinal)
        }

    /**
     * Получить настройку темной темы
     */
    val darkTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }

    /**
     * Получить настройку динамических цветов (Material You)
     */
    val dynamicColor: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: false
        }

    /**
     * Следовать ли системной теме
     */
    val systemTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[SYSTEM_THEME_KEY] ?: true
        }

    /**
     * Сохранить вариант темы
     */
    suspend fun setThemeVariant(variant: ThemeVariant) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_VARIANT_KEY] = variant.ordinal
        }
    }

    /**
     * Сохранить настройку темной темы
     */
    suspend fun setDarkTheme(dark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = dark
        }
    }

    /**
     * Сохранить настройку динамических цветов
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    /**
     * Сохранить настройку следования системной теме
     */
    suspend fun setSystemTheme(enabled: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[SYSTEM_THEME_KEY] = enabled
        }
    }

    /**
     * Сбросить все настройки темы к значениям по умолчанию
     */
    suspend fun resetToDefaults() {
        context.themeDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
