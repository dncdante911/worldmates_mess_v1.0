package com.worldmates.messenger.ui.messages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.ui.media.FullscreenImageViewer
import com.worldmates.messenger.ui.media.ImageGalleryViewer
import com.worldmates.messenger.ui.media.InlineVideoPlayer
import com.worldmates.messenger.ui.media.MiniAudioPlayer
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.data.model.ReactionGroup
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.ui.theme.WMShapes
import com.worldmates.messenger.ui.theme.MessageBubbleOwn
import com.worldmates.messenger.ui.theme.MessageBubbleOther
import com.worldmates.messenger.ui.theme.WMGradients
import com.worldmates.messenger.ui.theme.AnimatedGradientBackground
import com.worldmates.messenger.ui.theme.WMColors
import com.worldmates.messenger.ui.theme.rememberThemeState
import com.worldmates.messenger.ui.theme.PresetBackground
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var isCurrentlyTyping by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var replyToMessage by remember { mutableStateOf<Message?>(null) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val themeState = rememberThemeState()

    // 📸 Галерея фото - збір всіх фото з чату
    var showImageGallery by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    val imageUrls = remember(messages) {
        messages.mapNotNull { message ->
            // Шукаємо URL медіа в різних полях
            val mediaUrl = message.mediaUrl ?: message.decryptedMediaUrl ?: message.decryptedText
            if (mediaUrl != null && isImageUrl(mediaUrl)) {
                mediaUrl
            } else null
        }
    }

    // 🎵 Мінімізований аудіо плеєр
    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()
    val showMiniPlayer = playbackState !is com.worldmates.messenger.utils.VoicePlayer.PlaybackState.Idle

    // Логування стану теми
    LaunchedEffect(themeState) {
        Log.d("MessagesScreen", "=== THEME STATE ===")
        Log.d("MessagesScreen", "Variant: ${themeState.variant}")
        Log.d("MessagesScreen", "IsDark: ${themeState.isDark}")
        Log.d("MessagesScreen", "BackgroundImageUri: ${themeState.backgroundImageUri}")
        Log.d("MessagesScreen", "PresetBackgroundId: ${themeState.presetBackgroundId}")
        Log.d("MessagesScreen", "==================")
    }

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

    // Фон з підтримкою кастомних зображень та preset градієнтів
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Застосування фону в залежності від налаштувань
        when {
            // Кастомне зображення
            themeState.backgroundImageUri != null -> {
                Log.d("MessagesScreen", "Applying custom background image: ${themeState.backgroundImageUri}")
                AsyncImage(
                    model = Uri.parse(themeState.backgroundImageUri),
                    contentDescription = "Chat background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f  // Напівпрозорість для кращої читабельності
                )
            }
            // Preset градієнт
            themeState.presetBackgroundId != null -> {
                Log.d("MessagesScreen", "Applying preset background: ${themeState.presetBackgroundId}")
                val preset = PresetBackground.fromId(themeState.presetBackgroundId)
                if (preset != null) {
                    Log.d("MessagesScreen", "Preset found: ${preset.displayName}")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = preset.gradientColors.map { it.copy(alpha = 0.3f) }
                                )
                            )
                    )
                } else {
                    Log.e("MessagesScreen", "Preset not found for ID: ${themeState.presetBackgroundId}")
                }
            }
            // Стандартний фон з теми
            else -> {
                Log.d("MessagesScreen", "Using default MaterialTheme background")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        // Контент поверх фону
        Column(
            modifier = Modifier.fillMaxSize()
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
                        voicePlayer = voicePlayer,
                        replyToMessage = replyToMessage,
                        onLongPress = {
                            selectedMessage = message
                            showContextMenu = true
                        },
                        onImageClick = { imageUrl ->
                            // Знаходимо індекс вибраного фото в списку
                            selectedImageIndex = imageUrls.indexOf(imageUrl).coerceAtLeast(0)
                            showImageGallery = true
                        },
                        onReply = { msg ->
                            // Встановлюємо повідомлення для відповіді
                            replyToMessage = msg
                        },
                        onToggleReaction = { messageId, emoji ->
                            viewModel.toggleReaction(messageId, emoji)
                        }
                    )
                }
            }

            // 📸 ГАЛЕРЕЯ ФОТО
            if (showImageGallery && imageUrls.isNotEmpty()) {
                ImageGalleryViewer(
                    imageUrls = imageUrls,
                    initialPage = selectedImageIndex,
                    onDismiss = { showImageGallery = false }
                )
            }

        // Message Context Menu Bottom Sheet
        if (showContextMenu && selectedMessage != null) {
            MessageContextMenu(
                message = selectedMessage!!,
                onDismiss = {
                    showContextMenu = false
                    selectedMessage = null
                },
                onReply = { message ->
                    replyToMessage = message
                    showContextMenu = false
                    selectedMessage = null
                },
                onForward = { message ->
                    // TODO: Implement forward to another chat
                    android.widget.Toast.makeText(
                        context,
                        "Переслання: ${message.decryptedText}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    showContextMenu = false
                    selectedMessage = null
                },
                onDelete = { message ->
                    viewModel.deleteMessage(message.id)
                    showContextMenu = false
                    selectedMessage = null
                },
                onCopy = { message ->
                    message.decryptedText?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        android.widget.Toast.makeText(
                            context,
                            "Текст скопійовано",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showContextMenu = false
                    selectedMessage = null
                }
            )
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

        // Reply Indicator
        ReplyIndicator(
            replyToMessage = replyToMessage,
            onCancelReply = { replyToMessage = null }
        )

        // 🎵 Мінімізований аудіо плеєр
        if (showMiniPlayer) {
            MiniAudioPlayer(
                audioUrl = "",
                audioTitle = "Аудіо повідомлення",
                isPlaying = playbackState is com.worldmates.messenger.utils.VoicePlayer.PlaybackState.Playing,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPauseClick = {
                    scope.launch {
                        if (playbackState is com.worldmates.messenger.utils.VoicePlayer.PlaybackState.Playing) {
                            voicePlayer.pause()
                        } else {
                            voicePlayer.resume()
                        }
                    }
                },
                onSeek = { position ->
                    voicePlayer.seek(position)
                },
                onClose = {
                    voicePlayer.stop()
                }
            )
        }

        // Message Input
        MessageInputBar(
            messageText = messageText,
            onMessageChange = { messageText = it },
            onSendClick = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText, replyToMessage?.id)
                    messageText = ""
                    replyToMessage = null  // Очищаємо reply після відправки
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
            showMediaOptions = showMediaOptions,
            showEmojiPicker = showEmojiPicker,
            onToggleEmojiPicker = { showEmojiPicker = !showEmojiPicker },
            showStickerPicker = showStickerPicker,
            onToggleStickerPicker = { showStickerPicker = !showStickerPicker }
        )

        // 🎭 Sticker Picker
        if (showStickerPicker) {
            com.worldmates.messenger.ui.components.StickerPicker(
                onStickerSelected = { sticker ->
                    viewModel.sendSticker(sticker.id)
                    showStickerPicker = false
                },
                onDismiss = { showStickerPicker = false }
            )
        }
        }  // Кінець Column
    }  // Кінець Box
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
    val colorScheme = MaterialTheme.colorScheme

    // Telegram-style AppBar - четкий и читаемый
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
                    Text(recipientName, color = colorScheme.onPrimary)
                    if (isTyping) {
                        Text(
                            text = "печатает...",
                            fontSize = 12.sp,
                            color = colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    tint = colorScheme.onPrimary
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.primary,  // Цвет темы
            titleContentColor = colorScheme.onPrimary,
            navigationIconContentColor = colorScheme.onPrimary,
            actionIconContentColor = colorScheme.onPrimary
        )
    )  // Конец TopAppBar
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleComposable(
    message: Message,
    voicePlayer: VoicePlayer,
    replyToMessage: Message? = null,
    onLongPress: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onReply: (Message) -> Unit = {},
    onToggleReaction: (Long, String) -> Unit = { _, _ -> }
) {
    val isOwn = message.fromId == UserSession.userId
    val colorScheme = MaterialTheme.colorScheme

    // 💬 Свайп для Reply
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeDistance = 100f  // Максимальна відстань свайпу

    // ❤️ Реакції
    var showReactionPicker by remember { mutableStateOf(false) }

    // Групуємо реакції по емоджі для відображення
    val reactionGroups = remember(message.reactions) {
        message.reactions?.groupBy { it.reaction }?.map { (emoji, reactionList) ->
            ReactionGroup(
                emoji = emoji,
                count = reactionList.size,
                userIds = reactionList.map { it.userId },
                hasMyReaction = reactionList.any { it.userId == UserSession.userId }
            )
        } ?: emptyList()
    }

    // Цвета из темы
    val bgColor = if (isOwn) {
        colorScheme.primary
    } else {
        colorScheme.surfaceVariant
    }
    val textColor = if (isOwn) {
        colorScheme.onPrimary
    } else {
        colorScheme.onSurfaceVariant
    }

    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()

    var showVideoPlayer by remember { mutableStateOf(false) }

    // 💬 Обгортка з іконкою Reply для свайпу
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Іконка Reply (показується при свайпі)
        if (offsetX > 20f) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                tint = colorScheme.primary.copy(alpha = (offsetX / maxSwipeDistance).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(if (isOwn) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
                    .size(24.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > maxSwipeDistance / 2) {
                                // Свайп достатньо далеко - викликаємо reply
                                onReply(message)
                            }
                            // Повертаємо на місце
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Свайп тільки праворуч для reply
                            offsetX = (offsetX + dragAmount).coerceIn(0f, maxSwipeDistance)
                        }
                    )
                },
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            // Современный Material 3 пузырь с тенью и скруглениями
            Column {
            Card(
            modifier = Modifier
                .widthIn(max = 280.dp)  // Оптимальная ширина для читабельности
                .padding(horizontal = 8.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showReactionPicker = true }
                ),
            shape = if (isOwn) {
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 4.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 20.dp
                )
            },
            colors = CardDefaults.cardColors(
                containerColor = bgColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 12.dp,  // Увеличенный padding для лучшей читабельности
                    vertical = 8.dp      // Более комфортный вертикальный отступ
                )
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

                // 💬 Цитата Reply (якщо є)
                if (message.replyToId != null && message.replyToText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = textColor.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Вертикальна лінія
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(40.dp)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            // Текст цитати
                            Column {
                                Text(
                                    text = "Відповідь",
                                    color = colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = message.replyToText!!,
                                    color = textColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Text message
                if (shouldShowText) {
                    Text(
                        text = message.decryptedText!!,
                        color = textColor,
                        fontSize = 16.sp,  // Увеличенный размер для лучшей читабельності
                        lineHeight = 22.sp,  // Улучшенный межстрочный интервал
                        style = MaterialTheme.typography.bodyLarge
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
                            .clickable { onImageClick(effectiveMediaUrl) },
                        contentScale = ContentScale.Crop
                    )
                }

                // Video - інлайн плеєр
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
                    InlineVideoPlayer(
                        videoUrl = effectiveMediaUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (shouldShowText) 8.dp else 0.dp),
                        onFullscreenClick = {
                            // Відкриваємо повноекранний плеєр
                            showVideoPlayer = true
                        }
                    )

                    // Повноекранний плеєр (опціонально)
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

                // Время с более стильным форматированием + галочки прочитано
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(message.timeStamp),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // ✓✓ Галочки прочитано (тільки для власних повідомлень)
                    if (isOwn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(
                            isRead = message.is_read ?: false,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

            // ❤️ Реакції під повідомленням
            MessageReactions(
                reactions = reactionGroups,
                onReactionClick = { emoji ->
                    onToggleReaction(message.id, emoji)
                },
                modifier = Modifier.align(if (isOwn) Alignment.End else Alignment.Start)
            )
        }  // Закриття Column
        }  // Закриття Row

        // 🎯 ReactionPicker overlay (показується при довгому натисканні)
        if (showReactionPicker) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)  // Зміщення вгору над повідомленням
            ) {
                ReactionPicker(
                    onReactionSelected = { emoji ->
                        onToggleReaction(message.id, emoji)
                        showReactionPicker = false
                    },
                    onDismiss = { showReactionPicker = false }
                )
            }
        }
    }  // Закриття Box зі свайпом
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
    showMediaOptions: Boolean,
    showEmojiPicker: Boolean,
    onToggleEmojiPicker: () -> Unit,
    showStickerPicker: Boolean,
    onToggleStickerPicker: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)  // Цвет из темы
            .navigationBarsPadding()  // Отступ от кнопок навигации
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
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp),
                    placeholder = {
                        Text(
                            "Введіть повідомлення...",
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    ),
                    leadingIcon = {
                        IconButton(onClick = onShowMediaOptions) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // 😊 Кнопка емоджі
                IconButton(onClick = onToggleEmojiPicker) {
                    Icon(
                        imageVector = if (showEmojiPicker) Icons.Default.KeyboardArrowDown else Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                // 🎭 Кнопка стікерів
                IconButton(onClick = onToggleStickerPicker) {
                    Icon(
                        imageVector = if (showStickerPicker) Icons.Default.KeyboardArrowDown else Icons.Default.StickyNote2,
                        contentDescription = "Stickers",
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                if (messageText.isNotBlank()) {
                    IconButton(
                        onClick = onSendClick,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (isLoading) colorScheme.onSurfaceVariant else colorScheme.primary
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
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        }

        // 😊 Emoji Picker
        if (showEmojiPicker) {
            com.worldmates.messenger.ui.components.EmojiPicker(
                onEmojiSelected = { emoji ->
                    onMessageChange(messageText + emoji)
                },
                onDismiss = onToggleEmojiPicker
            )
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

/**
 * Контекстне меню для повідомлень (Reply, Forward, Delete, Copy)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    message: Message,
    onDismiss: () -> Unit,
    onReply: (Message) -> Unit,
    onForward: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onCopy: (Message) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // Заголовок
            Text(
                text = "Дії з повідомленням",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = colorScheme.onSurface
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Reply
            ContextMenuItem(
                icon = Icons.Default.Reply,
                text = "Відповісти",
                onClick = { onReply(message) }
            )

            // Forward
            ContextMenuItem(
                icon = Icons.Default.Forward,
                text = "Переслати",
                onClick = { onForward(message) }
            )

            // Copy (якщо є текст)
            if (!message.decryptedText.isNullOrEmpty()) {
                ContextMenuItem(
                    icon = Icons.Default.ContentCopy,
                    text = "Копіювати текст",
                    onClick = { onCopy(message) }
                )
            }

            // Delete (тільки для своїх повідомлень)
            if (message.fromId == UserSession.userId) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "Видалити",
                    onClick = { onDelete(message) },
                    isDestructive = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Елемент контекстного меню
 */
@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = if (isDestructive) {
        Color(0xFFD32F2F)  // Червоний для видалення
    } else {
        colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

/**
 * Індикатор повідомлення, на яке відповідаємо
 */
@Composable
fun ReplyIndicator(
    replyToMessage: Message?,
    onCancelReply: () -> Unit
) {
    if (replyToMessage != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (replyToMessage.fromId == UserSession.userId) "Ви" else "Користувач",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = replyToMessage.decryptedText ?: "[Медіа]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelReply) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Скасувати відповідь",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * ❤️ Панель вибору реакцій емоджі (з'являється при довгому тапі)
 */
@Composable
fun ReactionPicker(
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val reactions = listOf("❤️", "👍", "😂", "😮", "😢", "🙏", "🔥", "👏")

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            reactions.forEach { emoji ->
                Surface(
                    onClick = {
                        onReactionSelected(emoji)
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = emoji,
                            fontSize = 28.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 👍 Показ реакцій під повідомленням
 */
@Composable
fun MessageReactions(
    reactions: List<ReactionGroup>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isNotEmpty()) {
        Row(
            modifier = modifier
                .padding(top = 4.dp, start = 8.dp)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            reactions.forEach { reactionGroup ->
                Surface(
                    onClick = { onReactionClick(reactionGroup.emoji) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (reactionGroup.hasMyReaction) {
                        Color(0xFF0084FF).copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = if (reactionGroup.hasMyReaction) {
                        BorderStroke(1.dp, Color(0xFF0084FF))
                    } else null,
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reactionGroup.emoji,
                            fontSize = 14.sp
                        )
                        if (reactionGroup.count > 1) {
                            Text(
                                text = reactionGroup.count.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (reactionGroup.hasMyReaction) {
                                    Color(0xFF0084FF)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✓✓ Індикатор статусу повідомлення (прочитано/доставлено)
 */
@Composable
fun MessageStatusIcon(
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)  // Накладання галочок
    ) {
        // Перша галочка
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = if (isRead) "Прочитано" else "Відправлено",
            tint = if (isRead) Color(0xFF0084FF) else Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        // Друга галочка (тільки коли доставлено або прочитано)
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = if (isRead) "Прочитано" else "Доставлено",
            tint = if (isRead) Color(0xFF0084FF) else Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Перевірка чи URL вказує на зображення
 */
private fun isImageUrl(url: String): Boolean {
    val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
    val lowerUrl = url.lowercase()
    return imageExtensions.any { lowerUrl.contains(it) } ||
           lowerUrl.contains("image") ||
           lowerUrl.contains("/img/") ||
           lowerUrl.contains("/images/")
}
}