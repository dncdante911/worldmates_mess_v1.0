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
            onBackPressed = onBackPressed,
            onCallClick = { /* TODO: Реалізувати звонки */ },
            onInfoClick = { /* TODO: Інформація про чат */ }
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
                    fileManager = fileManager
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
                onLocationClick = { /* TODO */ },
                onFileClick = { /* TODO */ }
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

@Composable
fun MessagesTopBar(
    recipientName: String,
    recipientAvatar: String,
    onBackPressed: () -> Unit,
    onCallClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp)
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
                    Text("Онлайн", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
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

@Composable
fun MessageBubbleRow(
    message: Message,
    voicePlayer: VoicePlayer,
    fileManager: FileManager
) {
    val isOwn = message.fromId == UserSession.userId
    val bgColor = if (isOwn) Color(0xFF0084FF) else Color(0xFFE5E5EA)
    val textColor = if (isOwn) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp),
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

                // Text content
                if (message.decryptedText != null && message.decryptedText!!.isNotEmpty()) {
                    Text(
                        text = message.decryptedText!!,
                        color = textColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = if (message.mediaUrl != null) 8.dp else 0.dp)
                    )
                }

                // Image
                if (message.type == Constants.MESSAGE_TYPE_IMAGE && !message.mediaUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Video thumbnail
                if (message.type == Constants.MESSAGE_TYPE_VIDEO && !message.mediaUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f)),
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

                // Voice message
                if (message.type == Constants.MESSAGE_TYPE_VOICE && !message.mediaUrl.isNullOrEmpty()) {
                    VoiceMessagePlayerUI(
                        message = message,
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
}

@Composable
fun VoiceMessagePlayerUI(
    message: Message,
    voicePlayer: VoicePlayer,
    textColor: Color
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
                        voicePlayer.play(message.mediaUrl!!)
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
fun MediaOptionsBar(
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
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
            Box(contentAlignment = Alignment.Center) {
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
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}