# Готовый MessagesScreen.kt патч

## Инструкция: Скопируйте этот код в нужные места

### ШАГИ:

---

## ШАГ 1: Добавить импорты (после строки 26)

Найдите:
```kotlin
import coil.compose.AsyncImage
import com.worldmates.messenger.data.Constants
```

**ДОБАВЬТЕ ПОСЛЕ:**
```kotlin
import com.worldmates.messenger.ui.media.FullscreenImageViewer
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
```

---

## ШАГ 2: Добавить состояния в MessageBubbleComposable

Найдите функцию `MessageBubbleComposable` (около строки 282):
```kotlin
@Composable
fun MessageBubbleComposable(
    message: Message,
    voicePlayer: VoicePlayer
) {
    val isOwn = message.fromId == UserSession.userId
    val bgColor = if (isOwn) MessageBubbleOwn else MessageBubbleOther
    val textColor = if (isOwn) Color.White else Color.Black
    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()

    // <-- ДОБАВЬТЕ ЭТИ 3 СТРОКИ СЮДА:
    var showFullscreenImage by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }

    Row(
```

---

## ШАГ 3: Добавить клик на изображение

Найдите блок изображения (около строки 343):
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
            .padding(top = if (shouldShowText) 8.dp else 0.dp),  // <-- ДОБАВЬТЕ .clickable ПОСЛЕ ЭТОЙ СТРОКИ
        contentScale = ContentScale.Crop
    )
}
```

**ЗАМЕНИТЕ на:**
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
            .clickable { showFullscreenImage = true },  // <-- ДОБАВЛЕНА ЭТА СТРОКА
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

---

## ШАГ 4: Добавить клик на видео

Найдите блок видео (около строки 357):
```kotlin
// Video (thumbnail)
if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.3f))
            .padding(top = if (shouldShowText) 8.dp else 0.dp),  // <-- ДОБАВЬТЕ .clickable ПОСЛЕ ЭТОЙ СТРОКИ
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

**ЗАМЕНИТЕ на:**
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
            .clickable { showVideoPlayer = true },  // <-- ДОБАВЛЕНА ЭТА СТРОКА
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

---

## ГОТОВО! Теперь:

1. **Нажмите Sync Gradle** в Android Studio
2. **Rebuild Project**
3. **Запустите приложение**

### Результат:
- ✅ При клике на изображение откроется полноэкранный просмотр с zoom
- ✅ При клике на видео откроется видеоплеер
- ✅ Автообновление после отправки медиа работает
- ✅ Ошибка "не отримано відповідь" исправлена

---

## Если не хотите копировать вручную:

Я могу создать полный файл `MessagesScreen.kt` целиком. Скажите, нужен ли полный файл?
