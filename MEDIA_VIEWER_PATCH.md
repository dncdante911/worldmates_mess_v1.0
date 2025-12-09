# Патч для добавления просмотра медиа в MessagesScreen.kt

Примените эти изменения в `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt`:

## 1. Добавить импорты (после строки 26):

```kotlin
import com.worldmates.messenger.ui.media.FullscreenImageViewer
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
```

## 2. В функции `MessageBubbleComposable` после строки 291 добавить:

```kotlin
    // Состояние для полноэкранного просмотра медиа
    var showFullscreenImage by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
```

## 3. Заменить блок "Image" (около строки 343-354):

### БЫЛО:
```kotlin
                // Image - показываем если тип "image" или если URL указывает на изображение
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "image") {
                    AsyncImage(
                        model = effectiveMediaUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(top = if (shouldShowText) 8.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                }
```

### СТАЛО:
```kotlin
                // Image - показываем если тип "image" или если URL указывает на изображение
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "image") {
                    AsyncImage(
                        model = effectiveMediaUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(top = if (shouldShowText) 8.dp else 0.dp)
                            .clickable { showFullscreenImage = true },
                        contentScale = ContentScale.Crop
                    )

                    // Полноэкранный просмотр изображения
                    if (showFullscreenImage) {
                        FullscreenImageViewer(
                            imageUrl = effectiveMediaUrl,
                            onDismiss = { showFullscreenImage = false }
                        )
                    }
                }
```

## 4. Заменить блок "Video" (около строки 356-374):

### БЫЛО:
```kotlin
                // Video (thumbnail)
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .padding(top = if (shouldShowText) 8.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
```

### СТАЛО:
```kotlin
                // Video (thumbnail)
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .padding(top = if (shouldShowText) 8.dp else 0.dp)
                            .clickable { showVideoPlayer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Полноэкранный видеоплеер
                    if (showVideoPlayer) {
                        FullscreenVideoPlayer(
                            videoUrl = effectiveMediaUrl,
                            onDismiss = { showVideoPlayer = false }
                        )
                    }
                }
```

## Применение патча вручную:

Откройте файл `MessagesScreen.kt` в Android Studio и примените изменения вручную, следуя инструкциям выше.

После применения:
1. Sync Gradle
2. Rebuild Project
3. Запустите приложение

Теперь при клике на изображение или видео откроется полноэкранный просмотр с возможностью:
- Увеличения изображения (pinch-to-zoom)
- Воспроизведения видео с контролами
- Закрытия по кнопке X или назад
