package com.worldmates.messenger.ui.theme

import android.content.Context
import android.util.Log
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

private const val TAG = "ThemeRepository"

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
            val variant = ThemeVariant.fromOrdinal(ordinal)
            Log.d(TAG, "Reading theme variant: ${variant.name}")
            variant
        }

    /**
     * Получить настройку темной темы
     */
    val darkTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            val dark = preferences[DARK_THEME_KEY] ?: false
            Log.d(TAG, "Reading dark theme: $dark")
            dark
        }

    /**
     * Получить настройку динамических цветов (Material You)
     */
    val dynamicColor: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            val dynamic = preferences[DYNAMIC_COLOR_KEY] ?: false
            Log.d(TAG, "Reading dynamic color: $dynamic")
            dynamic
        }

    /**
     * Следовать ли системной теме
     */
    val systemTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            val system = preferences[SYSTEM_THEME_KEY] ?: true
            Log.d(TAG, "Reading system theme: $system")
            system
        }

    /**
     * Сохранить вариант темы
     */
    suspend fun setThemeVariant(variant: ThemeVariant) {
        Log.d(TAG, "Saving theme variant: ${variant.name}")
        context.themeDataStore.edit { preferences ->
            preferences[THEME_VARIANT_KEY] = variant.ordinal
        }
    }

    /**
     * Сохранить настройку темной темы
     */
    suspend fun setDarkTheme(dark: Boolean) {
        Log.d(TAG, "Saving dark theme: $dark")
        context.themeDataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = dark
        }
    }

    /**
     * Сохранить настройку динамических цветов
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        Log.d(TAG, "Saving dynamic color: $enabled")
        context.themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    /**
     * Сохранить настройку следования системной теме
     */
    suspend fun setSystemTheme(enabled: Boolean) {
        Log.d(TAG, "Saving system theme: $enabled")
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
