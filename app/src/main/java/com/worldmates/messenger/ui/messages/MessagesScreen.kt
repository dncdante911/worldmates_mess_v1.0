package com.worldmates.messenger.ui.messages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.ui.media.FullscreenImageViewer
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.ui.theme.WMShapes
import com.worldmates.messenger.ui.theme.MessageBubbleOwn
import com.worldmates.messenger.ui.theme.MessageBubbleOther
import com.worldmates.messenger.utils.VoiceRecorder
import com.worldmates.messenger.utils.VoicePlayer
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    fileManager: FileManager,
    voiceRecorder: VoiceRecorder,
    voicePlayer: VoicePlayer,
    recipientName: String,
    recipientAvatar: String,
    isGroup: Boolean,
    onBackPressed: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onVideoSelected: (Uri) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val recordingState by voiceRecorder.recordingState.collectAsState()
    val recordingDuration by voiceRecorder.recordingDuration.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val isOnline by viewModel.recipientOnlineStatus.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showMediaOptions by remember { mutableStateOf(false) }
    var isCurrentlyTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Управление индикатором "печатает" с автоматическим сбросом через 2 секунды
    LaunchedEffect(messageText) {
        if (messageText.isNotBlank() && !isCurrentlyTyping) {
            // Начали печатать
            viewModel.sendTypingStatus(true)
            isCurrentlyTyping = true
        } else if (messageText.isBlank() && isCurrentlyTyping) {
            // Очистили поле
            viewModel.sendTypingStatus(false)
            isCurrentlyTyping = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onVideoSelected(it) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("MessagesScreen", "Вибрано аудіо: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "audio")
            } else {
                Log.e("MessagesScreen", "Не вдалося скопіювати аудіо файл")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("MessagesScreen", "Вибрано файл: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "file")
            } else {
                Log.e("MessagesScreen", "Не вдалося скопіювати файл")
            }
        }
    }

    // Для выбора нескольких файлов (до 15 штук)
    val multipleFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size > Constants.MAX_FILES_PER_MESSAGE) {
                Log.w("MessagesScreen", "Вибрано занадто багато файлів: ${uris.size}, макс: ${Constants.MAX_FILES_PER_MESSAGE}")
                // TODO: показать ошибку пользователю
            } else {
                // TODO: обработать множественные файлы
                Log.d("MessagesScreen", "Вибрано ${uris.size} файлів")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        MessagesHeaderBar(
            recipientName = recipientName,
            recipientAvatar = recipientAvatar,
            isOnline = isOnline,
            isTyping = isTyping,
            onBackPressed = onBackPressed
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubbleComposable(
                    message = message,
                    voicePlayer = voicePlayer
                )
            }
        }

        // Upload Progress
        if (uploadProgress > 0 && uploadProgress < 100) {
            LinearProgressIndicator(
                progress = uploadProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }

        // Message Input
        MessageInputBar(
            messageText = messageText,
            onMessageChange = { messageText = it },
            onSendClick = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                }
            },
            isLoading = isLoading,
            recordingState = recordingState,
            recordingDuration = recordingDuration,
            voiceRecorder = voiceRecorder,
            onStartVoiceRecord = {
                scope.launch {
                    voiceRecorder.startRecording()
                }
            },
            onCancelVoiceRecord = {
                scope.launch {
                    voiceRecorder.cancelRecording()
                }
            },
            onStopVoiceRecord = {
                scope.launch {
                    val stopped = voiceRecorder.stopRecording()
                    if (stopped && voiceRecorder.recordingState.value is VoiceRecorder.RecordingState.Completed) {
                        val filePath = (voiceRecorder.recordingState.value as VoiceRecorder.RecordingState.Completed).filePath
                        viewModel.uploadAndSendMedia(java.io.File(filePath), "voice")
                    }
                }
            },
            onShowMediaOptions = { showMediaOptions = !showMediaOptions },
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onPickVideo = { videoPickerLauncher.launch("video/*") },
            onPickAudio = { audioPickerLauncher.launch("audio/*") },
            onPickFile = { filePickerLauncher.launch("*/*") },
            showMediaOptions = showMediaOptions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesHeaderBar(
    recipientName: String,
    recipientAvatar: String,
    isOnline: Boolean,
    isTyping: Boolean,
    onBackPressed: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Аватар с индикатором онлайн-статуса
                if (recipientAvatar.isNotEmpty()) {
                    Box {
                        AsyncImage(
                            model = recipientAvatar,
                            contentDescription = recipientName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Зелёная/серая точка онлайн-статуса
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Имя и статус "печатает"
                Column {
                    Text(recipientName)
                    if (isTyping) {
                        Text(
                            text = "печатает...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0084FF),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

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

    var showFullscreenImage by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp),
            shape = if (isOwn) WMShapes.ownMessageBubble else WMShapes.otherMessageBubble,
            color = bgColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Получаем URL медиа из разных источников
                // 1. Сначала пытаемся использовать decryptedMediaUrl
                var effectiveMediaUrl = message.decryptedMediaUrl

                // 2. Если пусто, проверяем mediaUrl
                if (effectiveMediaUrl.isNullOrEmpty()) {
                    effectiveMediaUrl = message.mediaUrl
                }

                // 3. Если все еще пусто, пытаемся извлечь URL из decryptedText
                if (effectiveMediaUrl.isNullOrEmpty() && !message.decryptedText.isNullOrEmpty()) {
                    effectiveMediaUrl = extractMediaUrlFromText(message.decryptedText!!)
                }

                // Определяем тип медиа по URL (для случаев, когда message.type == "text")
                val detectedMediaType = detectMediaType(effectiveMediaUrl, message.type)

                // 🔍 ДЕТАЛЬНЕ ЛОГУВАННЯ ДЛЯ ВІДЛАДКИ
                Log.d("MessageBubble", """
                    ========== ПОВІДОМЛЕННЯ ==========
                    ID: ${message.id}
                    Type: ${message.type}
                    DecryptedText: ${message.decryptedText}
                    MediaUrl: ${message.mediaUrl}
                    DecryptedMediaUrl: ${message.decryptedMediaUrl}
                    EffectiveMediaUrl: $effectiveMediaUrl
                    DetectedMediaType: $detectedMediaType
                    ==================================
                """.trimIndent())

                // Показываем текст только если это не чистый URL медиа
                val shouldShowText = message.decryptedText != null &&
                    message.decryptedText!!.isNotEmpty() &&
                    !isOnlyMediaUrl(message.decryptedText!!) &&
                    detectedMediaType == "text"  // Не показываем текст, если это URL медиа

                // Text message
                if (shouldShowText) {
                    Text(
                        text = message.decryptedText!!,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

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

                // Voice/Audio message player
                if (!effectiveMediaUrl.isNullOrEmpty() &&
                    (detectedMediaType == "voice" || detectedMediaType == "audio")) {
                    VoiceMessagePlayer(
                        message = message,
                        voicePlayer = voicePlayer,
                        textColor = textColor,
                        mediaUrl = effectiveMediaUrl
                    )
                }

                // File attachment
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "file") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (shouldShowText) 8.dp else 0.dp)
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = effectiveMediaUrl.substringAfterLast("/"),
                            color = textColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = formatTime(message.timeStamp),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(
    message: Message,
    voicePlayer: VoicePlayer,
    textColor: Color,
    mediaUrl: String
) {
    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    if (playbackState == VoicePlayer.PlaybackState.Playing) {
                        voicePlayer.pause()
                    } else {
                        voicePlayer.play(mediaUrl)
                    }
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (playbackState == VoicePlayer.PlaybackState.Playing)
                    Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { voicePlayer.seek(it.toLong()) },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
        )

        Text(
            text = voicePlayer.formatTime(duration),
            color = textColor,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun MessageInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    recordingState: VoiceRecorder.RecordingState,
    recordingDuration: Long,
    voiceRecorder: VoiceRecorder,
    onStartVoiceRecord: () -> Unit,
    onCancelVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> Unit,
    onShowMediaOptions: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onPickFile: () -> Unit,
    showMediaOptions: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Media Options
        if (showMediaOptions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaOptionButton(
                    icon = Icons.Default.Image,
                    label = "Фото",
                    onClick = { onPickImage() }
                )
                MediaOptionButton(
                    icon = Icons.Default.VideoLibrary,
                    label = "Відео",
                    onClick = { onPickVideo() }
                )
                MediaOptionButton(
                    icon = Icons.Default.AudioFile,
                    label = "Аудіо",
                    onClick = { onPickAudio() }
                )
                MediaOptionButton(
                    icon = Icons.Default.InsertDriveFile,
                    label = "Файл",
                    onClick = { onPickFile() }
                )
                MediaOptionButton(
                    icon = Icons.Default.LocationOn,
                    label = "Локація",
                    onClick = { }
                )
                MediaOptionButton(
                    icon = Icons.Default.AttachMoney,
                    label = "Оплата",
                    onClick = { }
                )
            }
        }

        // Voice Recording UI
        if (recordingState is VoiceRecorder.RecordingState.Recording || 
            recordingState is VoiceRecorder.RecordingState.Paused) {
            VoiceRecordingBar(
                duration = recordingDuration,
                voiceRecorder = voiceRecorder,
                onCancel = onCancelVoiceRecord,
                onStop = onStopVoiceRecord,
                isRecording = recordingState is VoiceRecorder.RecordingState.Recording
            )
        }

        // Message Input
        if (recordingState !is VoiceRecorder.RecordingState.Recording &&
            recordingState !is VoiceRecorder.RecordingState.Paused) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp),
                    placeholder = { Text("Введіть повідомлення...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    leadingIcon = {
                        IconButton(onClick = onShowMediaOptions) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                        }
                    }
                )

                if (messageText.isNotBlank()) {
                    IconButton(
                        onClick = onSendClick,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (isLoading) Color.Gray else Color(0xFF0084FF)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onStartVoiceRecord,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = Color(0xFF0084FF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceRecordingBar(
    duration: Long,
    voiceRecorder: VoiceRecorder,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    isRecording: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Recording",
            tint = Color.Red,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = voiceRecorder.formatDuration(duration),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
        }

        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF))
        ) {
            Text("Надіслати", color = Color.White)
        }
    }
}

@Composable
fun MediaOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF0084FF),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}

/**
 * Определяет тип медиа по URL или явному типу сообщения.
 * Если message.type указан явно (не "text"), используем его.
 * Иначе определяем по расширению файла или пути в URL.
 */
private fun detectMediaType(url: String?, messageType: String): String {
    // Если тип явно указан и это не "text", используем его
    if (messageType != "text" && messageType.isNotEmpty()) {
        return messageType
    }

    // Если URL пустой, возвращаем "text"
    if (url.isNullOrEmpty()) {
        return "text"
    }

    val lowerUrl = url.lowercase()

    // Определяем по расширению файла
    return when {
        // Изображения
        lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
        lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
        lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".bmp") ||
        lowerUrl.contains("/upload/photos/") -> "image"

        // Видео
        lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
        lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".avi") ||
        lowerUrl.endsWith(".mkv") || lowerUrl.contains("/upload/videos/") -> "video"

        // Аудио/Голос
        lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") ||
        lowerUrl.endsWith(".ogg") || lowerUrl.endsWith(".m4a") ||
        lowerUrl.endsWith(".aac") || lowerUrl.contains("/upload/sounds/") -> "audio"

        // Файлы
        lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".doc") ||
        lowerUrl.endsWith(".docx") || lowerUrl.endsWith(".xls") ||
        lowerUrl.endsWith(".xlsx") || lowerUrl.endsWith(".zip") ||
        lowerUrl.endsWith(".rar") || lowerUrl.contains("/upload/files/") -> "file"

        else -> "text"
    }
}

/**
 * Извлекает URL медиа-файла из текста сообщения.
 * Возвращает URL если он найден, иначе null.
 */
private fun extractMediaUrlFromText(text: String): String? {
    val trimmed = text.trim()

    // Проверяем, является ли весь текст URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        val lowerText = trimmed.lowercase()
        if (lowerText.contains("/upload/photos/") ||
            lowerText.contains("/upload/videos/") ||
            lowerText.contains("/upload/sounds/") ||
            lowerText.contains("/upload/files/") ||
            lowerText.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|mp4|webm|mov|mp3|wav|ogg|pdf|doc|docx)$"))) {
            return trimmed
        }
    }

    // Пытаемся найти URL медиа внутри текста
    val urlPattern = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
    val match = urlPattern.find(trimmed)

    return match?.value?.let { url ->
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains("/upload/photos/") ||
            lowerUrl.contains("/upload/videos/") ||
            lowerUrl.contains("/upload/sounds/") ||
            lowerUrl.contains("/upload/files/") ||
            lowerUrl.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|mp4|webm|mov|mp3|wav|ogg|pdf|doc|docx)$"))) {
            url
        } else {
            null
        }
    }
}

/**
 * Проверяет, является ли текст только URL медиа-файла.
 * Если да, не нужно показывать текст отдельно (покажем только медиа).
 */
private fun isOnlyMediaUrl(text: String): Boolean {
    val trimmed = text.trim()

    // Если текст не похож на URL, это не чистый URL
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return false
    }

    // Проверяем, содержит ли URL только медиа-ресурс без дополнительного текста
    val lowerText = trimmed.lowercase()
    val isMediaUrl = lowerText.contains("/upload/photos/") ||
        lowerText.contains("/upload/videos/") ||
        lowerText.contains("/upload/sounds/") ||
        lowerText.contains("/upload/files/") ||
        lowerText.endsWith(".jpg") ||
        lowerText.endsWith(".jpeg") ||
        lowerText.endsWith(".png") ||
        lowerText.endsWith(".gif") ||
        lowerText.endsWith(".mp4") ||
        lowerText.endsWith(".mp3") ||
        lowerText.endsWith(".webm")

    // Если это URL медиа и нет дополнительного текста после URL
    return isMediaUrl && !trimmed.contains(" ") && !trimmed.contains("\n")
}