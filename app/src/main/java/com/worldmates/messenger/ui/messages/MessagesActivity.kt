package com.worldmates.messenger.ui.messages

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.ui.media.FullscreenImageViewer
import com.worldmates.messenger.ui.media.FullscreenVideoPlayer
import com.worldmates.messenger.ui.theme.WorldMatesTheme
import com.worldmates.messenger.utils.VoiceRecorder
import com.worldmates.messenger.utils.VoicePlayer
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessagesActivity : AppCompatActivity() {

    private lateinit var viewModel: MessagesViewModel
    private lateinit var fileManager: FileManager
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var voicePlayer: VoicePlayer

    private var recipientId: Long = 0
    private var groupId: Long = 0
    private var recipientName: String = ""
    private var recipientAvatar: String = ""
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Отримуємо параметри
        recipientId = intent.getLongExtra("recipient_id", 0)
        groupId = intent.getLongExtra("group_id", 0)
        recipientName = intent.getStringExtra("recipient_name") ?: "Unknown"
        recipientAvatar = intent.getStringExtra("recipient_avatar") ?: ""
        isGroup = intent.getBooleanExtra("is_group", false)

        // Ініціалізуємо утиліти
        fileManager = FileManager(this)
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer(this)
        
        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)

        // Завантажуємо повідомлення
        if (isGroup) {
            viewModel.initializeGroup(groupId)
        } else {
            viewModel.initialize(recipientId)
        }

        setContent {
            WorldMatesTheme {
                MessagesScreenContent(
                    activity = this,
                    viewModel = viewModel,
                    fileManager = fileManager,
                    voiceRecorder = voiceRecorder,
                    voicePlayer = voicePlayer,
                    recipientName = recipientName,
                    recipientAvatar = recipientAvatar,
                    isGroup = isGroup,
                    groupId = groupId,
                    onBackPressed = { finish() }
                )
            }
        }

        Log.d("MessagesActivity", "Init: recipient=$recipientId, group=$groupId, isGroup=$isGroup")
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePlayer.release()
        fileManager.clearOldCacheFiles()
    }
}

@Composable
fun MessagesScreenContent(
    activity: MessagesActivity,
    viewModel: MessagesViewModel,
    fileManager: FileManager,
    voiceRecorder: VoiceRecorder,
    voicePlayer: VoicePlayer,
    recipientName: String,
    recipientAvatar: String,
    isGroup: Boolean,
    groupId: Long,
    onBackPressed: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val recordingState by voiceRecorder.recordingState.collectAsState()
    val recordingDuration by voiceRecorder.recordingDuration.collectAsState()
    val error by viewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showMediaOptions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, Constants.MESSAGE_TYPE_IMAGE)
            }
        }
        showMediaOptions = false
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, Constants.MESSAGE_TYPE_VIDEO)
            }
        }
        showMediaOptions = false
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d("MessagesActivity", "Вибрано аудіо: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "audio")
            } else {
                Log.e("MessagesActivity", "Не вдалося скопіювати аудіо файл")
            }
        }
        showMediaOptions = false
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d("MessagesActivity", "Вибрано файл: $it")
            val file = fileManager.copyUriToCache(it)
            if (file != null) {
                viewModel.uploadAndSendMedia(file, "file")
            } else {
                Log.e("MessagesActivity", "Не вдалося скопіювати файл")
            }
        }
        showMediaOptions = false
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Log.w("MessagesActivity", "Some permissions denied")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        MessagesTopBar(
            recipientName = recipientName,
            recipientAvatar = recipientAvatar,
            isGroup = isGroup,
            onBackPressed = onBackPressed,
            onCallClick = { /* TODO: Реалізувати звонки */ },
            onInfoClick = { /* TODO: Інформація про чат */ },
            onGroupNameClick = {
                if (isGroup && groupId != 0L) {
                    val intent = Intent(activity, com.worldmates.messenger.ui.groups.GroupDetailsActivity::class.java)
                    intent.putExtra("group_id", groupId)
                    activity.startActivity(intent)
                }
            }
        )

        // Error message
        if (error != null) {
            Surface(
                color = Color(0xFFFFCDD2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    error!!,
                    color = Color(0xFFC62828),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Messages list
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true,
            state = listState
        ) {
            if (isLoading && messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            items(messages.reversed()) { message ->
                MessageBubbleRow(
                    message = message,
                    voicePlayer = voicePlayer,
                    fileManager = fileManager,
                    viewModel = viewModel
                )
            }
        }

        // Upload progress
        if (uploadProgress > 0 && uploadProgress < 100) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = uploadProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
                Text(
                    "Завантажено: $uploadProgress%",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(4.dp, 2.dp)
                )
            }
        }

        // Media options
        if (showMediaOptions) {
            MediaOptionsBar(
                onImageClick = { imagePickerLauncher.launch("image/*") },
                onVideoClick = { videoPickerLauncher.launch("video/*") },
                onAudioClick = { audioPickerLauncher.launch("audio/*") },
                onLocationClick = { /* TODO */ },
                onFileClick = { filePickerLauncher.launch("*/*") }
            )
        }

        // Voice recording UI
        if (recordingState is VoiceRecorder.RecordingState.Recording ||
            recordingState is VoiceRecorder.RecordingState.Paused
        ) {
            VoiceRecordingUI(
                duration = recordingDuration,
                voiceRecorder = voiceRecorder,
                isRecording = recordingState is VoiceRecorder.RecordingState.Recording,
                onCancel = {
                    scope.launch {
                        voiceRecorder.cancelRecording()
                    }
                },
                onSend = {
                    scope.launch {
                        voiceRecorder.stopRecording()
                        val state = voiceRecorder.recordingState.value
                        if (state is VoiceRecorder.RecordingState.Completed) {
                            viewModel.uploadAndSendMedia(
                                File(state.filePath),
                                Constants.MESSAGE_TYPE_VOICE
                            )
                        }
                    }
                }
            )
        }

        // Message input
        if (recordingState !is VoiceRecorder.RecordingState.Recording &&
            recordingState !is VoiceRecorder.RecordingState.Paused
        ) {
            MessageInputBar(
                messageText = messageText,
                onTextChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                        showMediaOptions = false
                    }
                },
                onAttachClick = { showMediaOptions = !showMediaOptions },
                onVoiceClick = {
                    scope.launch {
                        voiceRecorder.startRecording()
                    }
                },
                isLoading = isLoading
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopBar(
    recipientName: String,
    recipientAvatar: String,
    isGroup: Boolean = false,
    onBackPressed: () -> Unit,
    onCallClick: () -> Unit,
    onInfoClick: () -> Unit,
    onGroupNameClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp)
                    .then(
                        if (isGroup) Modifier.clickable(onClick = onGroupNameClick)
                        else Modifier
                    )
            ) {
                if (recipientAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = recipientAvatar,
                        contentDescription = recipientName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(recipientName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (isGroup) "Переглянути деталі групи" else "Онлайн",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onCallClick) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White)
            }
            IconButton(onClick = onCallClick) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
            }
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0084FF)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: Message,
    voicePlayer: VoicePlayer,
    fileManager: FileManager,
    viewModel: MessagesViewModel
) {
    val isOwn = message.fromId == UserSession.userId
    val bgColor = if (isOwn) Color(0xFF0084FF) else Color(0xFFE5E5EA)
    val textColor = if (isOwn) Color.White else Color.Black

    var showFullscreenImage by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var showMessageMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showMessageMenu = true }
                ),
            shape = RoundedCornerShape(12.dp),
            color = bgColor
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender name (для груп)
                if (message.senderName != null && message.fromId != UserSession.userId) {
                    Text(
                        text = message.senderName!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ========== ЛОГІКА ВИЗНАЧЕННЯ МЕДІА ==========
                // 1. Пытаемся использовать decryptedMediaUrl
                var effectiveMediaUrl = message.decryptedMediaUrl

                // 2. Если пусто, проверяем mediaUrl
                if (effectiveMediaUrl.isNullOrEmpty()) {
                    effectiveMediaUrl = message.mediaUrl
                }

                // 3. Если все еще пусто, пытаемся извлечь URL из decryptedText
                if (effectiveMediaUrl.isNullOrEmpty() && !message.decryptedText.isNullOrEmpty()) {
                    effectiveMediaUrl = extractMediaUrlFromText(message.decryptedText!!)
                }

                // Определяем тип медиа
                val detectedMediaType = detectMediaType(effectiveMediaUrl, message.type)

                // 🔍 ДЕТАЛЬНЕ ЛОГУВАННЯ
                Log.d("MessageBubbleRow", """
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
                    detectedMediaType == "text"

                // Text content
                if (shouldShowText) {
                    Text(
                        text = message.decryptedText!!,
                        color = textColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = if (!effectiveMediaUrl.isNullOrEmpty()) 8.dp else 0.dp)
                    )
                }

                // ========== IMAGE ==========
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "image") {
                    AsyncImage(
                        model = effectiveMediaUrl,
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFullscreenImage = true },
                        contentScale = ContentScale.Crop
                    )

                    // Fullscreen viewer
                    if (showFullscreenImage) {
                        FullscreenImageViewer(
                            imageUrl = effectiveMediaUrl,
                            onDismiss = { showFullscreenImage = false }
                        )
                    }
                }

                // ========== VIDEO ==========
                if (!effectiveMediaUrl.isNullOrEmpty() && detectedMediaType == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
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

                    // Fullscreen video player
                    if (showVideoPlayer) {
                        FullscreenVideoPlayer(
                            videoUrl = effectiveMediaUrl,
                            onDismiss = { showVideoPlayer = false }
                        )
                    }
                }

                // ========== VOICE/AUDIO ==========
                if (!effectiveMediaUrl.isNullOrEmpty() &&
                    (detectedMediaType == "voice" || detectedMediaType == "audio" ||
                     message.type == Constants.MESSAGE_TYPE_VOICE)) {
                    VoiceMessagePlayerUI(
                        mediaUrl = effectiveMediaUrl,
                        voicePlayer = voicePlayer,
                        textColor = textColor
                    )
                }

                // Timestamp
                Text(
                    text = formatTime(message.timeStamp),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    // ========== МЕНЮ ПОВІДОМЛЕННЯ (ДОВГЕ НАТИСКАННЯ) ==========
    if (showMessageMenu) {
        AlertDialog(
            onDismissRequest = { showMessageMenu = false },
            title = { Text("Дії з повідомленням") },
            text = {
                Column {
                    // Редагувати (тільки свої текстові повідомлення)
                    if (isOwn && message.decryptedText?.isNotEmpty() == true) {
                        TextButton(
                            onClick = {
                                editText = message.decryptedText ?: ""
                                showMessageMenu = false
                                showEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Редагувати")
                        }
                    }

                    // Видалити
                    TextButton(
                        onClick = {
                            showMessageMenu = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        Spacer(Modifier.width(8.dp))
                        Text("Видалити", color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMessageMenu = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // ========== ДІАЛОГ РЕДАГУВАННЯ ==========
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редагувати повідомлення") },
            text = {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть текст...") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) {
                            viewModel.editMessage(message.id, editText)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // ========== ДІАЛОГ ВИДАЛЕННЯ ==========
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити повідомлення?") },
            text = {
                Text(
                    if (isOwn)
                        "Це повідомлення буде видалено для всіх учасників розмови."
                    else
                        "Це повідомлення буде видалено тільки для вас."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(message.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Видалити", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun VoiceMessagePlayerUI(
    mediaUrl: String,
    voicePlayer: VoicePlayer,
    textColor: Color
) {
    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()
    val currentPlayingUrl by voicePlayer.currentPlayingUrl.collectAsState()
    val scope = rememberCoroutineScope()

    // Визначаємо, чи це саме той трек, що зараз грає
    val isThisTrackPlaying = currentPlayingUrl == mediaUrl
    val isPlaying = isThisTrackPlaying && playbackState == VoicePlayer.PlaybackState.Playing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    if (isPlaying) {
                        voicePlayer.pause()
                    } else {
                        voicePlayer.play(mediaUrl)
                    }
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = if (isThisTrackPlaying) currentPosition.toFloat() else 0f,
            onValueChange = { if (isThisTrackPlaying) voicePlayer.seek(it.toLong()) },
            valueRange = 0f..(if (isThisTrackPlaying) duration.toFloat() else 100f),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
        )

        Text(
            text = voicePlayer.formatTime(if (isThisTrackPlaying) duration else 0),
            color = textColor,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun MediaOptionsBar(
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLocationClick: () -> Unit,
    onFileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color.White)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaOptionButton(Icons.Default.Image, "Фото", onImageClick)
        MediaOptionButton(Icons.Default.VideoLibrary, "Відео", onVideoClick)
        MediaOptionButton(Icons.Default.AudioFile, "Аудіо", onAudioClick)
        MediaOptionButton(Icons.Default.LocationOn, "Локація", onLocationClick)
        MediaOptionButton(Icons.Default.AttachFile, "Файл", onFileClick)
    }
}

@Composable
fun VoiceRecordingUI(
    duration: Long,
    voiceRecorder: VoiceRecorder,
    isRecording: Boolean,
    onCancel: () -> Unit,
    onSend: () -> Unit
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
        }

        Button(
            onClick = onSend,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Надіслати", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun MessageInputBar(
    messageText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
        }

        TextField(
            value = messageText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp)),
            placeholder = { Text("Введіть повідомлення...") },
            singleLine = false,
            maxLines = 4,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (messageText.isNotBlank()) {
            IconButton(onClick = onSendClick, enabled = !isLoading) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF0084FF))
            }
        } else {
            IconButton(onClick = onVoiceClick, enabled = !isLoading) {
                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF0084FF))
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}

/**
 * Определяет тип медиа по URL или явному типу сообщения
 */
private fun detectMediaType(url: String?, messageType: String): String {
    // Игнорируем все текстовые типы
    val isTextType = messageType.isEmpty() ||
                     messageType == "text" ||
                     messageType == "right_text" ||
                     messageType == "left_text"

    // Конвертируем left/right варианты в базовые типы
    val actualType = when (messageType) {
        "left_image", "right_image" -> "image"
        "left_video", "right_video" -> "video"
        "left_audio", "right_audio", "left_voice", "right_voice" -> "audio"
        "left_file", "right_file" -> "file"
        else -> messageType
    }

    // Если получили базовый тип (не текстовый), используем его
    if (!isTextType && actualType != messageType && actualType.isNotEmpty()) {
        return actualType
    }

    // Если URL пустой, возвращаем "text"
    if (url.isNullOrEmpty()) {
        return "text"
    }

    val lowerUrl = url.lowercase()

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

        // Аудио
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
 * Извлекает URL медиа-файла из текста сообщения
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
 * Проверяет, является ли текст только URL медиа-файла
 */
private fun isOnlyMediaUrl(text: String): Boolean {
    val trimmed = text.trim()

    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        return false
    }

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

    return isMediaUrl && !trimmed.contains(" ") && !trimmed.contains("\n")
}