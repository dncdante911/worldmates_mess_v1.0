package com.worldmates.messenger.ui.messages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.ui.theme.WorldMatesTheme
import com.worldmates.messenger.utils.VoiceRecorder
import com.worldmates.messenger.utils.VoicePlayer
import kotlinx.coroutines.launch

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

        recipientId = intent.getLongExtra("recipient_id", 0)
        groupId = intent.getLongExtra("group_id", 0)
        recipientName = intent.getStringExtra("recipient_name") ?: "Unknown"
        recipientAvatar = intent.getStringExtra("recipient_avatar") ?: ""
        isGroup = intent.getBooleanExtra("is_group", false)

        fileManager = FileManager(this)
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer(this)

        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)

        if (isGroup) {
            viewModel.initializeGroup(groupId)
        } else {
            viewModel.initialize(recipientId)
        }

        setContent {
            WorldMatesTheme {
                MessagesScreen(
                    viewModel = viewModel,
                    fileManager = fileManager,
                    voiceRecorder = voiceRecorder,
                    voicePlayer = voicePlayer,
                    recipientName = recipientName,
                    recipientAvatar = recipientAvatar,
                    isGroup = isGroup,
                    onBackPressed = { finish() },
                    onImageSelected = { uri ->
                        val file = fileManager.copyUriToCache(uri)
                        if (file != null) {
                            viewModel.uploadAndSendMedia(file, "image")
                        }
                    },
                    onVideoSelected = { uri ->
                        val file = fileManager.copyUriToCache(uri)
                        if (file != null) {
                            viewModel.uploadAndSendMedia(file, "video")
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePlayer.release()
    }
}

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

    var messageText by remember { mutableStateOf("") }
    var showMediaOptions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        MessagesHeaderBar(
            recipientName = recipientName,
            recipientAvatar = recipientAvatar,
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
            showMediaOptions = showMediaOptions
        )
    }
}

@Composable
fun MessagesHeaderBar(
    recipientName: String,
    recipientAvatar: String,
    onBackPressed: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
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
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(recipientName)
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
    val bgColor = if (isOwn) Color(0xFF0084FF) else Color.White
    val textColor = if (isOwn) Color.White else Color.Black
    val playbackState by voicePlayer.playbackState.collectAsState()
    val currentPosition by voicePlayer.currentPosition.collectAsState()
    val duration by voicePlayer.duration.collectAsState()

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
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Text message
                if (message.decryptedText != null && message.decryptedText!!.isNotEmpty()) {
                    Text(
                        text = message.decryptedText!!,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

                // Image
                if (!message.mediaUrl.isNullOrEmpty() && message.type == "image") {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(top = if (message.decryptedText != null) 8.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                // Video (thumbnail)
                if (!message.mediaUrl.isNullOrEmpty() && message.type == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .padding(top = if (message.decryptedText != null) 8.dp else 0.dp),
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

                // Voice message player
                if (message.type == "voice" && !message.mediaUrl.isNullOrEmpty()) {
                    VoiceMessagePlayer(
                        message = message,
                        voicePlayer = voicePlayer,
                        textColor = textColor
                    )
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
    icon: androidx.compose.material.icons.Icons.Filled,
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}