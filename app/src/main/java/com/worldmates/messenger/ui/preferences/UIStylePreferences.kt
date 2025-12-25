package com.worldmates.messenger.ui.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Enum для типів UI стилів
 */
enum class UIStyle {
    WORLDMATES,  // Карточний стиль з градієнтами та анімаціями
    TELEGRAM     // Список у стилі Telegram (мінімалістичний)
}

/**
 * Менеджер налаштувань UI стилю
 */
object UIStylePreferences {
    private const val PREFS_NAME = "ui_style_preferences"
    private const val KEY_UI_STYLE = "ui_style"
    private const val KEY_BUBBLE_STYLE = "bubble_style"

    private var _currentStyle = MutableStateFlow(UIStyle.WORLDMATES)
    val currentStyle: StateFlow<UIStyle> = _currentStyle

    private var _currentBubbleStyle = MutableStateFlow(BubbleStyle.STANDARD)
    val currentBubbleStyle: StateFlow<BubbleStyle> = _currentBubbleStyle

    /**
     * Ініціалізація з SharedPreferences
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // UI Style
        val styleName = prefs.getString(KEY_UI_STYLE, UIStyle.WORLDMATES.name)
        _currentStyle.value = try {
            UIStyle.valueOf(styleName ?: UIStyle.WORLDMATES.name)
        } catch (e: IllegalArgumentException) {
            UIStyle.WORLDMATES
        }

        // Bubble Style
        val bubbleStyleName = prefs.getString(KEY_BUBBLE_STYLE, BubbleStyle.STANDARD.name)
        _currentBubbleStyle.value = try {
            BubbleStyle.valueOf(bubbleStyleName ?: BubbleStyle.STANDARD.name)
        } catch (e: IllegalArgumentException) {
            BubbleStyle.STANDARD
        }
    }

    /**
     * Встановити новий стиль UI
     */
    fun setStyle(context: Context, style: UIStyle) {
        _currentStyle.value = style
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UI_STYLE, style.name).apply()
    }

    /**
     * Встановити новий стиль бульбашок
     */
    fun setBubbleStyle(context: Context, style: BubbleStyle) {
        _currentBubbleStyle.value = style
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BUBBLE_STYLE, style.name).apply()
    }

    /**
     * Отримати поточний стиль
     */
    fun getStyle(): UIStyle {
        return _currentStyle.value
    }

    /**
     * Отримати поточний стиль бульбашок
     */
    fun getBubbleStyle(): BubbleStyle {
        return _currentBubbleStyle.value
    }
}

/**
 * Composable для отримання поточного стилю
 */
@Composable
fun rememberUIStyle(): UIStyle {
    val style by UIStylePreferences.currentStyle.collectAsState()
    return style
}

/**
 * Composable для отримання поточного стилю бульбашок
 */
@Composable
fun rememberBubbleStyle(): BubbleStyle {
    val style by UIStylePreferences.currentBubbleStyle.collectAsState()
    return style
}
