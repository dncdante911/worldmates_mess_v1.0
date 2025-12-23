package com.worldmates.messenger.ui.theme

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Компонент для отображения кастомного фонового изображения или градиента
 * @param backgroundImageUri URI кастомного изображения (если установлено)
 * @param defaultGradient градиент по умолчанию (если изображение не установлено)
 * @param content контент, который будет отображен поверх фона
 */
@Composable
fun BackgroundContainer(
    backgroundImageUri: String?,
    defaultGradient: Brush,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Фоновый слой
        if (backgroundImageUri != null) {
            // Показываем кастомное изображение с затемнением
            AsyncImage(
                model = Uri.parse(backgroundImageUri),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Полупрозрачный оверлей для лучшей читабельности
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            // Показываем градиент темы
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = defaultGradient)
            )
        }

        // Контент поверх фона
        content()
    }
}
