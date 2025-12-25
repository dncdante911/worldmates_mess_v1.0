package com.worldmates.messenger.ui.messages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    fileManager: FileManager,
    voiceRecorder: VoiceRecorder,
    voicePlayer: VoicePlayer,
    recipientName: String,
    recipientAvatar: String,
    isGroup: Boolean,
    onBackPressed: () -> Unit
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
    var editingMessage by remember { mutableStateOf<Message?>(null) }
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
        uri?.let {
            Log.d("MessagesScreen", "Вибрано зображення: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "image")
            } else {
                Log.e("MessagesScreen", "Не вдалося скопіювати зображення")
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("MessagesScreen", "Вибрано відео: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "video")
            } else {
                Log.e("MessagesScreen", "Не вдалося скопіювати відео")
            }
        }
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
                onBackPressed = onBackPressed,
                onUserProfileClick = {
                    Log.d("MessagesScreen", "Відкриваю профіль користувача: $recipientName")
                    // TODO: Відкрити повний профіль користувача
                    android.widget.Toast.makeText(context, "Профіль: $recipientName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onCallClick = {
                    Log.d("MessagesScreen", "Аудіо дзвінок до: $recipientName")
                    android.widget.Toast.makeText(context, "Дзвінок до $recipientName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onVideoCallClick = {
                    Log.d("MessagesScreen", "Відеодзвінок до: $recipientName")
                    android.widget.Toast.makeText(context, "Відеодзвінок до $recipientName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onSearchClick = {
                    Log.d("MessagesScreen", "Пошук в чаті")
                    android.widget.Toast.makeText(context, "Пошук в чаті", android.widget.Toast.LENGTH_SHORT).show()
                },
                onMuteClick = {
                    Log.d("MessagesScreen", "Вимкнення сповіщень для: $recipientName")
                    // TODO: Реалізувати збереження налаштувань сповіщень
                    android.widget.Toast.makeText(context, "Сповіщення вимкнено для $recipientName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onClearHistoryClick = {
                    Log.d("MessagesScreen", "Очищення історії чату")
                    // TODO: Реалізувати viewModel.clearChatHistory() в MessagesViewModel
                    android.widget.Toast.makeText(context, "Очищення історії поки недоступне", android.widget.Toast.LENGTH_SHORT).show()
                },
                onChangeWallpaperClick = {
                    Log.d("MessagesScreen", "Зміна фону чату")
                    // TODO: Відкрити вибір фону
                    android.widget.Toast.makeText(context, "Вибір фону поки недоступний", android.widget.Toast.LENGTH_SHORT).show()
                }
            )

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                items(
                    items = messages.reversed(),
                    key = { it.id }
                ) { message ->
                    // ✨ Анімація появи повідомлення
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 4 }
                        ) + fadeIn(
                            initialAlpha = 0.3f
                        ),
                        modifier = Modifier.animateItemPlacement()
                    ) {
                    MessageBubbleComposable(
                        message = message,
                        voicePlayer = voicePlayer,
                        replyToMessage = replyToMessage,
                        onLongPress = {
                            selectedMessage = message
                            showContextMenu = true
                            // 🧪 ТЕСТОВЕ ПОВІДОМЛЕННЯ - переконайся що довгий тап працює!
                            android.widget.Toast.makeText(
                                context,
                                "🎯 Довгий тап працює! Меню має відкритись!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
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
                    }  // Закриття AnimatedVisibility
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
                onEdit = { message ->
                    // 🧪 ТЕСТОВЕ ПОВІДОМЛЕННЯ
                    android.widget.Toast.makeText(
                        context,
                        "✏️ Редагування розпочато! Текст: ${message.decryptedText?.take(20)}...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    editingMessage = message
                    messageText = message.decryptedText ?: ""
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

        // Edit Indicator
        EditIndicator(
            editingMessage = editingMessage,
            onCancelEdit = {
                editingMessage = null
                messageText = ""
            }
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
                    if (editingMessage != null) {
                        // 🧪 ТЕСТОВЕ ПОВІДОМЛЕННЯ
                        android.widget.Toast.makeText(
                            context,
                            "💾 Зберігаю зміни для повідомлення ID: ${editingMessage!!.id}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        // Редагуємо повідомлення
                        viewModel.editMessage(editingMessage!!.id, messageText)
                        messageText = ""
                        editingMessage = null
                    } else {
                        // Надсилаємо нове повідомлення
                        viewModel.sendMessage(messageText, replyToMessage?.id)
                        messageText = ""
                        replyToMessage = null  // Очищаємо reply після відправки
                    }
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

        // 😊 Emoji Picker
        if (showEmojiPicker) {
            com.worldmates.messenger.ui.components.EmojiPicker(
                onEmojiSelected = { emoji ->
                    messageText += emoji
                    // Не закриваємо picker автоматично, щоб можна було вибрати кілька емоджі
                },
                onDismiss = { showEmojiPicker = false }
            )
        }

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
    onBackPressed: () -> Unit,
    onUserProfileClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onClearHistoryClick: () -> Unit = {},
    onChangeWallpaperClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var showUserMenu by remember { mutableStateOf(false) }

    // Telegram-style AppBar - четкий и читаемый
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onUserProfileClick() }
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
                            text = "печатає...",
                            fontSize = 12.sp,
                            color = colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    } else if (isOnline) {
                        Text(
                            text = "онлайн",
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
                    contentDescription = "Назад",
                    tint = colorScheme.onPrimary
                )
            }
        },
        actions = {
            // Кнопка пошуку
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Пошук",
                    tint = colorScheme.onPrimary
                )
            }

            // Кнопка дзвінка
            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Дзвінок",
                    tint = colorScheme.onPrimary
                )
            }

            // Кнопка меню (3 крапки)
            Box {
                IconButton(onClick = { showUserMenu = !showUserMenu }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Більше",
                        tint = colorScheme.onPrimary
                    )
                }

                // Випадаюче меню
                DropdownMenu(
                    expanded = showUserMenu,
                    onDismissRequest = { showUserMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Переглянути профіль") },
                        onClick = {
                            showUserMenu = false
                            onUserProfileClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Відеодзвінок") },
                        onClick = {
                            showUserMenu = false
                            onVideoCallClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.VideoCall, contentDescription = null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Вимкнути сповіщення") },
                        onClick = {
                            showUserMenu = false
                            onMuteClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Змінити обої") },
                        onClick = {
                            showUserMenu = false
                            onChangeWallpaperClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Очистити історію") },
                        onClick = {
                            showUserMenu = false
                            onClearHistoryClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
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
                .wrapContentWidth()  // Адаптивна ширина під контент
                .widthIn(min = 60.dp, max = 280.dp)  // Мін/макс ширина як в Telegram
                .padding(horizontal = 16.dp)  // Більший відступ з боків
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
                    horizontal = 10.dp,  // Компактний padding в стилі Telegram
                    vertical = 6.dp      // Менший вертикальний відступ
                )
            ) {
                // Получаем URL медиа из разных источников
                var effectiveMediaUrl: String? = null

                // 1. Сначала пытаемся использовать decryptedMediaUrl
                if (!message.decryptedMediaUrl.isNullOrEmpty()) {
                    effectiveMediaUrl = message.decryptedMediaUrl
                    Log.d("MessageBubble", "Використовую decryptedMediaUrl: $effectiveMediaUrl")
                }
                // 2. Если пусто, проверяем mediaUrl
                else if (!message.mediaUrl.isNullOrEmpty()) {
                    effectiveMediaUrl = message.mediaUrl
                    Log.d("MessageBubble", "Використовую mediaUrl: $effectiveMediaUrl")
                }
                // 3. Если все еще пусто, пытаемся извлечь URL из decryptedText
                else if (!message.decryptedText.isNullOrEmpty()) {
                    effectiveMediaUrl = extractMediaUrlFromText(message.decryptedText!!)
                    Log.d("MessageBubble", "Витягнуто з тексту: $effectiveMediaUrl")
                }

                // Определяем тип медиа по URL
                val detectedMediaType = detectMediaType(effectiveMediaUrl, message.type)
                Log.d("MessageBubble", "ID повідомлення: ${message.id}, Тип: ${message.type}, Визначений тип: $detectedMediaType, URL: $effectiveMediaUrl")

                // Показываем текст ТОЛЬКО если:
                // 1. Текст есть И не пустой
                // 2. И это НЕ чистый URL медиа (текст + медиа можно, чистый URL - нет)
                val shouldShowText = !message.decryptedText.isNullOrEmpty() &&
                    !isOnlyMediaUrl(message.decryptedText!!)

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
                        fontSize = 15.sp,  // Компактний розмір тексту
                        lineHeight = 20.sp,  // Компактний міжрядковий інтервал
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Image - показываем если тип "image" или если URL указывает на изображение
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "image") {
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()  // Адаптується під розмір зображення
                            .widthIn(max = 250.dp)  // Максимальна ширина для зображень
                            .heightIn(min = 120.dp, max = 300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(top = if (shouldShowText) 6.dp else 0.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                            .clickable { onImageClick(effectiveMediaUrl) }
                    ) {
                        AsyncImage(
                            model = effectiveMediaUrl,
                            contentDescription = "Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = {
                                Log.e("MessageBubble", "Помилка завантаження зображення: $effectiveMediaUrl, error: ${it.result.throwable}")
                            }
                        )
                    }
                }

                // Video - інлайн плеєр
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
                    InlineVideoPlayer(
                        videoUrl = effectiveMediaUrl,
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(max = 250.dp)
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
                            isRead = message.isRead ?: false,
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
    val colorScheme = MaterialTheme.colorScheme

    // Компактний аудіо плеєр в стилі Telegram
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(min = 200.dp, max = 240.dp),
        shape = RoundedCornerShape(18.dp),
        color = textColor.copy(alpha = 0.1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Кнопка відтворення
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = colorScheme.primary
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
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState == VoicePlayer.PlaybackState.Playing)
                            Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Прогрес + час
            Column(modifier = Modifier.weight(1f)) {
                // Слайдер прогресу
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { voicePlayer.seek(it.toLong()) },
                    valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primary,
                        activeTrackColor = colorScheme.primary,
                        inactiveTrackColor = textColor.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Час
            Text(
                text = voicePlayer.formatTime(if (currentPosition > 0) currentPosition else duration),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
/**
 * Telegram-style MessageInputBar
 * Одна кнопка для всіх опцій: медіа, емоджі, стікери
 */
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
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .navigationBarsPadding()
    ) {
        // Єдине спливаюче меню для всіх опцій (як в Telegram)
        if (showMediaOptions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                // Медіа опції
                Text(
                    text = "Вкласти",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Емоджі та Стікери
                Text(
                    text = "Додати",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaOptionButton(
                        icon = Icons.Default.EmojiEmotions,
                        label = "Емоджі",
                        onClick = {
                            onShowMediaOptions() // Закриваємо меню
                            scope.launch {
                                kotlinx.coroutines.delay(150) // Затримка 150мс для гарної анімації
                                if (!showEmojiPicker) {
                                    onToggleEmojiPicker() // Відкриваємо emoji picker
                                }
                            }
                        }
                    )
                    MediaOptionButton(
                        icon = Icons.Default.StickyNote2,
                        label = "Стікери",
                        onClick = {
                            onShowMediaOptions() // Закриваємо меню
                            scope.launch {
                                kotlinx.coroutines.delay(150) // Затримка 150мс для гарної анімації
                                if (!showStickerPicker) {
                                    onToggleStickerPicker() // Відкриваємо sticker picker
                                }
                            }
                        }
                    )
                }
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

        // Message Input - Telegram Style (компактний та повний)
        if (recordingState !is VoiceRecorder.RecordingState.Recording &&
            recordingState !is VoiceRecorder.RecordingState.Paused) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Кнопка "+"  - показує всі опції
                IconButton(
                    onClick = onShowMediaOptions,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (showMediaOptions) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Опції",
                        tint = if (showMediaOptions) colorScheme.primary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Поле введення - компактне та повне
                TextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                    placeholder = {
                        Text(
                            "Повідомлення",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Кнопка відправки або голосового запису
                if (messageText.isNotBlank()) {
                    // Кнопка відправки
                    IconButton(
                        onClick = onSendClick,
                        enabled = !isLoading,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Відправити",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Кнопка голосового запису
                    IconButton(
                        onClick = onStartVoiceRecord,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосове повідомлення",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
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
    // Если URL пустой, используем тип сообщения
    if (url.isNullOrEmpty()) {
        Log.d("detectMediaType", "URL пустий, тип повідомлення: $messageType")
        return if (messageType.isNotEmpty() && messageType != "text") messageType else "text"
    }

    val lowerUrl = url.lowercase()
    Log.d("detectMediaType", "Аналіз URL: $lowerUrl, тип повідомлення: $messageType")

    // Спочатку перевіряємо за шляхом (найнадійніше)
    val typeByPath = when {
        lowerUrl.contains("/upload/photos/") || lowerUrl.contains("/upload/images/") -> "image"
        lowerUrl.contains("/upload/videos/") -> "video"
        lowerUrl.contains("/upload/sounds/") || lowerUrl.contains("/upload/audio/") -> "audio"
        lowerUrl.contains("/upload/files/") -> "file"
        else -> null
    }

    if (typeByPath != null) {
        Log.d("detectMediaType", "Визначено за шляхом: $typeByPath")
        return typeByPath
    }

    // Потім перевіряємо за розширенням
    val typeByExtension = when {
        // Изображения
        lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
        lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
        lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".bmp") -> "image"

        // Видео
        lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm") ||
        lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".avi") ||
        lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".3gp") -> "video"

        // Аудио/Голос
        lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") ||
        lowerUrl.endsWith(".ogg") || lowerUrl.endsWith(".m4a") ||
        lowerUrl.endsWith(".aac") || lowerUrl.endsWith(".opus") -> "audio"

        // Файлы
        lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".doc") ||
        lowerUrl.endsWith(".docx") || lowerUrl.endsWith(".xls") ||
        lowerUrl.endsWith(".xlsx") || lowerUrl.endsWith(".zip") ||
        lowerUrl.endsWith(".rar") || lowerUrl.endsWith(".txt") -> "file"

        else -> null
    }

    if (typeByExtension != null) {
        Log.d("detectMediaType", "Визначено за розширенням: $typeByExtension")
        return typeByExtension
    }

    // Якщо нічого не знайшли, використовуємо messageType
    if (messageType.isNotEmpty() && messageType != "text") {
        Log.d("detectMediaType", "Використовую тип повідомлення: $messageType")
        return messageType
    }

    Log.d("detectMediaType", "Не вдалося визначити тип, повертаю 'text'")
    return "text"
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
 * Контекстне меню для повідомлень (Reply, Edit, Forward, Delete, Copy)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    message: Message,
    onDismiss: () -> Unit,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
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

            // Edit (тільки для своїх текстових повідомлень)
            if (message.fromId == UserSession.userId && !message.decryptedText.isNullOrEmpty()) {
                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = "Редагувати",
                    onClick = { onEdit(message) }
                )
            }

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
 * Індикатор повідомлення, яке редагується
 */
@Composable
fun EditIndicator(
    editingMessage: Message?,
    onCancelEdit: () -> Unit
) {
    if (editingMessage != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color(0xFFFFF3E0), // Помаранчевий відтінок для редагування
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
                            Color(0xFFFF9800), // Помаранчевий
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редагування",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Редагування повідомлення",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Text(
                        text = editingMessage.decryptedText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onCancelEdit) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Скасувати редагування",
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
                // ✨ Анімація scale для реакцій
                var isVisible by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )

                LaunchedEffect(Unit) {
                    isVisible = true
                }

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
                    modifier = Modifier
                        .height(28.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
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
 * Покращена видимість: більший розмір, яскравіші кольори, тінь
 */
@Composable
fun MessageStatusIcon(
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 1.dp,
                shape = CircleShape
            )
            .background(
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .padding(horizontal = 3.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy((-6).dp)  // Накладання галочок
    ) {
        // Перша галочка - більший розмір і краща видимість
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = if (isRead) "Прочитано" else "Відправлено",
            tint = if (isRead) Color(0xFF0084FF) else Color(0xFF8E8E93),  // Світліший сірий
            modifier = Modifier.size(16.dp)  // Збільшено з 14dp до 16dp
        )
        // Друга галочка (тільки коли доставлено або прочитано)
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = if (isRead) "Прочитано" else "Доставлено",
            tint = if (isRead) Color(0xFF0084FF) else Color(0xFF8E8E93),  // Світліший сірий
            modifier = Modifier.size(16.dp)  // Збільшено з 14dp до 16dp
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