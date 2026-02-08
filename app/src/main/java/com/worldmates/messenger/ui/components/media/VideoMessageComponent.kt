package com.worldmates.messenger.ui.components.media

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldmates.messenger.ui.media.InlineVideoPlayer
import com.worldmates.messenger.ui.video.AdvancedVideoPlayer

/**
 * Компонент для отображения видео в сообщении
 *
 * Извлечено из MessagesScreen.kt (строка 2010-2031) для уменьшения размера файла
 * Обновлено: использует новый AdvancedVideoPlayer с жестами управления
 *
 * @param videoUrl URL видеофайла
 * @param showTextAbove Есть ли текст над видео (для отступа)
 * @param enablePiP Включить Picture-in-Picture (по умолчанию true)
 * @param modifier Дополнительный модификатор
 */
@Composable
fun VideoMessageComponent(
    videoUrl: String,
    showTextAbove: Boolean = false,
    enablePiP: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showVideoPlayer by remember { mutableStateOf(false) }

    // Інлайн плеєр з превью
    InlineVideoPlayer(
        videoUrl = videoUrl,
        modifier = modifier
            .wrapContentWidth()
            .widthIn(max = 250.dp)
            .padding(top = if (showTextAbove) 8.dp else 0.dp),
        onFullscreenClick = {
            // Відкриваємо новий продвинутий плеєр
            showVideoPlayer = true
        }
    )

    // Повноекранний продвинутий плеєр з жестами
    if (showVideoPlayer) {
        AdvancedVideoPlayer(
            videoUrl = videoUrl,
            onDismiss = { showVideoPlayer = false },
            enablePiP = enablePiP,
            autoPlay = true
        )
    }
}

/**
 * Компонент для отображения видео в компактном режиме (например, в превью пересланных сообщений)
 */
@Composable
fun VideoMessagePreview(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onFullscreenClick: (() -> Unit)? = null
) {
    InlineVideoPlayer(
        videoUrl = videoUrl,
        modifier = modifier
            .width(120.dp)
            .height(90.dp),
        onFullscreenClick = onFullscreenClick ?: {}
    )
}
