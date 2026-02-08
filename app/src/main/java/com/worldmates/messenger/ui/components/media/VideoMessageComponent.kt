package com.worldmates.messenger.ui.components.media

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
import com.worldmates.messenger.ui.media.InlineVideoPlayer

/**
 * Компонент для отображения видео в сообщении
 *
 * Извлечено из MessagesScreen.kt (строка 2010-2031) для уменьшения размера файла
 *
 * @param videoUrl URL видеофайла
 * @param showTextAbove Есть ли текст над видео (для отступа)
 * @param modifier Дополнительный модификатор
 */
@Composable
fun VideoMessageComponent(
    videoUrl: String,
    showTextAbove: Boolean = false,
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
            // Відкриваємо повноекранний плеєр
            showVideoPlayer = true
        }
    )

    // Повноекранний плеєр (модальний)
    if (showVideoPlayer) {
        FullscreenVideoPlayer(
            videoUrl = videoUrl,
            onDismiss = { showVideoPlayer = false }
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
