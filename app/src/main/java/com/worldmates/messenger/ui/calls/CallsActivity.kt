package com.worldmates.messenger.ui.calls

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp
import com.worldmates.messenger.ui.settings.getSavedCallFrameStyle
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

/**
 * 🎨 Стилі кастомних рамок для відеодзвінків
 */
enum class CallFrameStyle {
    CLASSIC,    // Класична рамка з легкою тінню
    NEON,       // Неонова рамка з пульсуючим світінням
    GRADIENT,   // Градієнтна рамка з кольоровим переходом
    MINIMAL,    // Мінімалістична без рамки
    GLASS,      // Скляний ефект з blur
    RAINBOW     // Веселкова анімована рамка
}

class CallsActivity : ComponentActivity() {

    private lateinit var callsViewModel: CallsViewModel
    private var shouldInitiateCall = false
    private var callInitiated = false

    // 📋 Параметри дзвінка з Intent
    private var recipientId: Long = 0
    private var recipientName: String = ""
    private var recipientAvatar: String = ""
    private var callType: String = "audio"  // "audio" або "video"
    private var isGroup: Boolean = false
    private var groupId: Long = 0

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false)
            val cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false)

            if (audioGranted && (callType == "audio" || cameraGranted)) {
                // ✅ Дозволи отримано - ініціюємо дзвінок
                if (shouldInitiateCall && !callInitiated) {
                    initiateCall()
                }
            } else {
                // ❌ Дозволи не надано
                android.widget.Toast.makeText(
                    this,
                    "Для дзвінків потрібні дозволи на мікрофон" + if (callType == "video") " та камеру" else "",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем ThemeManager
        ThemeManager.initialize(this)

        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)

        // 📥 Отримати параметри з Intent
        recipientId = intent.getLongExtra("recipientId", 0)
        recipientName = intent.getStringExtra("recipientName") ?: "Користувач"
        recipientAvatar = intent.getStringExtra("recipientAvatar") ?: ""
        callType = intent.getStringExtra("callType") ?: "audio"
        isGroup = intent.getBooleanExtra("isGroup", false)
        groupId = intent.getLongExtra("groupId", 0)

        // Якщо є recipientId або groupId - потрібно ініціювати дзвінок
        shouldInitiateCall = (recipientId > 0 || groupId > 0)

        // Налаштувати Socket.IO listeners
        setupSocketListeners()

        // Запросити дозволи
        requestPermissions()

        setContent {
            WorldMatesThemedApp {
                CallsScreen(
                    callsViewModel,
                    this,
                    isInitiating = shouldInitiateCall && !callInitiated,
                    calleeName = recipientName,
                    calleeAvatar = recipientAvatar,
                    callType = callType
                )
            }
        }

        // Обробити завершення дзвінка
        callsViewModel.callEnded.observe(this) { ended ->
            if (ended == true) {
                finish()
            }
        }
    }

    /**
     * 📞 Ініціювати дзвінок
     */
    private fun initiateCall() {
        callInitiated = true
        android.util.Log.d("CallsActivity", "Ініціація дзвінка: recipientId=$recipientId, type=$callType, isGroup=$isGroup")

        if (isGroup && groupId > 0) {
            // Груповий дзвінок
            callsViewModel.initiateGroupCall(
                groupId = groupId.toInt(),
                groupName = recipientName,
                callType = callType
            )
        } else if (recipientId > 0) {
            // Особистий дзвінок
            callsViewModel.initiateCall(
                recipientId = recipientId.toInt(),
                recipientName = recipientName,
                recipientAvatar = recipientAvatar,
                callType = callType
            )
        }
    }

    /**
     * 🔌 Налаштувати Socket.IO listeners для вхідних подій
     */
    private fun setupSocketListeners() {
        android.util.Log.d("CallsActivity", "Налаштування Socket.IO listeners для дзвінків...")

        // 📞 Вхідний дзвінок
        callsViewModel.socketManager.on("call:incoming") { args ->
            try {
                if (args.isNotEmpty()) {
                    val data = args[0] as? org.json.JSONObject
                    data?.let {
                        android.util.Log.d("CallsActivity", "📞 Отримано вхідний дзвінок від ${it.optInt("fromId")}")

                        val callData = com.google.gson.JsonParser.parseString(data.toString()).asJsonObject
                        callsViewModel.onIncomingCall(callData)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CallsActivity", "Помилка обробки call:incoming", e)
            }
        }

        // ✅ Відповідь на дзвінок (SDP answer)
        callsViewModel.socketManager.on("call:answer") { args ->
            try {
                if (args.isNotEmpty()) {
                    val data = args[0] as? org.json.JSONObject
                    data?.let {
                        android.util.Log.d("CallsActivity", "✅ Отримано відповідь на дзвінок")

                        val answerData = com.google.gson.JsonParser.parseString(data.toString()).asJsonObject
                        callsViewModel.onCallAnswer(answerData)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CallsActivity", "Помилка обробки call:answer", e)
            }
        }

        // 🧊 ICE candidate
        callsViewModel.socketManager.on("ice:candidate") { args ->
            try {
                if (args.isNotEmpty()) {
                    val data = args[0] as? org.json.JSONObject
                    data?.let {
                        android.util.Log.d("CallsActivity", "🧊 Отримано ICE candidate")

                        val candidateData = com.google.gson.JsonParser.parseString(data.toString()).asJsonObject
                        callsViewModel.onIceCandidate(candidateData)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CallsActivity", "Помилка обробки ice:candidate", e)
            }
        }

        // ❌ Завершення дзвінка
        callsViewModel.socketManager.on("call:end") { args ->
            try {
                android.util.Log.d("CallsActivity", "❌ Дзвінок завершено")
                callsViewModel.endCall()
            } catch (e: Exception) {
                android.util.Log.e("CallsActivity", "Помилка обробки call:end", e)
            }
        }

        // 🚫 Відхилення дзвінка
        callsViewModel.socketManager.on("call:reject") { args ->
            try {
                android.util.Log.d("CallsActivity", "🚫 Дзвінок відхилено")
                callsViewModel.endCall()
            } catch (e: Exception) {
                android.util.Log.e("CallsActivity", "Помилка обробки call:reject", e)
            }
        }

        android.util.Log.d("CallsActivity", "✅ Socket.IO listeners налаштовано успішно")
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}

/**
 * Основний екран дзвінків
 */
@Composable
fun CallsScreen(
    viewModel: CallsViewModel,
    activity: CallsActivity,
    isInitiating: Boolean = false,
    calleeName: String = "",
    calleeAvatar: String = "",
    callType: String = "audio"
) {
    val incomingCall by viewModel.incomingCall.observeAsState()
    val callConnected by viewModel.callConnected.observeAsState(false)
    val callEnded by viewModel.callEnded.observeAsState(false)
    val remoteStream by viewModel.remoteStreamAdded.observeAsState()
    val connectionState by viewModel.connectionState.observeAsState("IDLE")
    val callError by viewModel.callError.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a))
    ) {
        when {
            incomingCall != null && !callConnected -> {
                // 📞 Вхідний дзвінок
                IncomingCallScreen(incomingCall!!, viewModel)
            }
            callConnected -> {
                // ✅ Активний дзвінок
                ActiveCallScreen(
                    viewModel = viewModel,
                    remoteStream = remoteStream,
                    connectionState = connectionState ?: "CONNECTING"
                )
            }
            isInitiating || (connectionState != "IDLE" && !callConnected) -> {
                // 📤 Вихідний дзвінок (ініціюємо або з'єднуємося)
                OutgoingCallScreen(
                    calleeName = calleeName,
                    calleeAvatar = calleeAvatar,
                    callType = callType,
                    viewModel = viewModel
                )
            }
            callError != null -> {
                // ❌ Помилка дзвінка
                ErrorScreen(callError!!, viewModel)
            }
            else -> {
                // ⏸️ Очікування
                IdleScreen(viewModel)
            }
        }
    }
}

/**
 * Екран очікування вхідного дзвінка
 */
@Composable
fun IncomingCallScreen(callData: CallData, viewModel: CallsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d0d0d)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Аватар що телефонує
        if (callData.fromAvatar.isNotEmpty()) {
            AsyncImage(
                model = callData.fromAvatar,
                contentDescription = callData.fromName,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF888888)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ім'я того, хто дзвонить
        Text(
            text = callData.fromName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Тип дзвінка
        Text(
            text = if (callData.callType == "video") "📹 Вхідний відеодзвінок" else "📞 Вхідний аудіодзвінок",
            fontSize = 16.sp,
            color = Color(0xFFbbbbbb),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Кнопки прийняття/відхилення
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка відхилення
            IconButton(
                onClick = { viewModel.rejectCall(callData.roomName) },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFd32f2f), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Reject",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Кнопка прийняття
            IconButton(
                onClick = { viewModel.acceptCall(callData) },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF4caf50), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Accept",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 📤 Екран вихідного дзвінка (дзвонимо...)
 */
@Composable
fun OutgoingCallScreen(
    calleeName: String,
    calleeAvatar: String,
    callType: String,
    viewModel: CallsViewModel
) {
    // Анімація пульсації
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0d0d0d)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Аватар
        if (calleeAvatar.isNotEmpty()) {
            AsyncImage(
                model = calleeAvatar,
                contentDescription = calleeName,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF888888)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ім'я
        Text(
            text = calleeName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Статус з анімацією
        Text(
            text = if (callType == "video") "📹 Відеодзвінок..." else "📞 Дзвонимо...",
            fontSize = 16.sp,
            color = Color(0xFFbbbbbb).copy(alpha = alpha),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Кнопка скасування
        IconButton(
            onClick = { viewModel.endCall() },
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFd32f2f), CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Cancel",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 📞 Екран активного дзвінка з кастомними рамками
 */
@Composable
fun ActiveCallScreen(
    viewModel: CallsViewModel,
    remoteStream: MediaStream?,
    connectionState: String
) {
    val context = LocalContext.current
    var audioEnabled by remember { mutableStateOf(true) }
    var videoEnabled by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }

    // 🎨 Завантажити збережений стиль рамки з Settings
    var currentFrameStyle by remember {
        mutableStateOf(getSavedCallFrameStyle(context))
    }
    val localStream by viewModel.localStreamAdded.observeAsState()

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            callDuration++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Віддалена відео/аудіо потік
        remoteStream?.let {
            if (it.videoTracks.isNotEmpty()) {
                // 🎥 Показати відео з кастомними рамками
                RemoteVideoView(
                    remoteStream = remoteStream,
                    localStream = localStream,
                    frameStyle = currentFrameStyle,
                    onSwitchCamera = { viewModel.switchCamera() }
                )
            } else {
                // Показати аватар під час аудіо дзвінка
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFF666666)
                    )
                }
            }
        }

        // Топ: інформація про дзвінок + перемикач стилів
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(callDuration),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = connectionState,
                    fontSize = 12.sp,
                    color = Color(0xFFbbbbbb),
                    modifier = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎨 Перемикач стилів рамок (тільки для відеодзвінків)
            if (remoteStream?.videoTracks?.isNotEmpty() == true) {
                FrameStyleSelector(
                    currentStyle = currentFrameStyle,
                    onStyleChange = { currentFrameStyle = it }
                )
            }
        }

        // Контрольні кнопки в низу
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Перемикач аудіо
            CallControlButton(
                icon = if (audioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                label = "Мік",
                isActive = audioEnabled,
                backgroundColor = if (audioEnabled) Color(0xFF2196F3) else Color(0xFF555555)
            ) {
                audioEnabled = !audioEnabled
                viewModel.toggleAudio(audioEnabled)
            }

            // Перемикач камери
            CallControlButton(
                icon = Icons.Default.Cameraswitch,
                label = "Камера",
                isActive = false,
                backgroundColor = Color(0xFF555555)
            ) {
                viewModel.switchCamera()
            }

            // Перемикач відео
            CallControlButton(
                icon = if (videoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = "Відео",
                isActive = videoEnabled,
                backgroundColor = if (videoEnabled) Color(0xFF2196F3) else Color(0xFF555555)
            ) {
                videoEnabled = !videoEnabled
                viewModel.toggleVideo(videoEnabled)
            }

            // Кнопка завершення дзвінка
            CallControlButton(
                icon = Icons.Default.CallEnd,
                label = "Завершити",
                isActive = false,
                backgroundColor = Color(0xFFd32f2f)
            ) {
                viewModel.endCall()
            }
        }
    }
}

/**
 * 🎨 Selector для вибору стилю рамки
 */
@Composable
fun FrameStyleSelector(
    currentStyle: CallFrameStyle,
    onStyleChange: (CallFrameStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(Color(0x99000000), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Style,
                contentDescription = "Frame Style",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentStyle.name,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CallFrameStyle.values().forEach { style ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val emoji = when (style) {
                                CallFrameStyle.CLASSIC -> "🎨"
                                CallFrameStyle.NEON -> "💡"
                                CallFrameStyle.GRADIENT -> "🌈"
                                CallFrameStyle.MINIMAL -> "⚪"
                                CallFrameStyle.GLASS -> "💎"
                                CallFrameStyle.RAINBOW -> "🌈"
                            }
                            Text("$emoji ${style.name}")
                        }
                    },
                    onClick = {
                        onStyleChange(style)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Компонент для кнопки управління дзвінком
 */
@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(backgroundColor, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 📹 Компонент для відображення віддаленої відео потоку з кастомними рамками
 */
@Composable
fun RemoteVideoView(
    remoteStream: MediaStream,
    localStream: MediaStream? = null,
    frameStyle: CallFrameStyle = CallFrameStyle.CLASSIC,
    onSwitchCamera: () -> Unit = {}
) {
    var isFullscreen by remember { mutableStateOf(false) }
    var pipOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap → fullscreen toggle
                        isFullscreen = !isFullscreen
                    }
                )
            }
    ) {
        // 🎥 Віддалене відео з кастомною рамкою
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) 0.dp else 16.dp)
        ) {
            // Застосовуємо стиль рамки
            when (frameStyle) {
                CallFrameStyle.CLASSIC -> ClassicVideoFrame(remoteStream)
                CallFrameStyle.NEON -> NeonVideoFrame(remoteStream)
                CallFrameStyle.GRADIENT -> GradientVideoFrame(remoteStream)
                CallFrameStyle.MINIMAL -> MinimalVideoFrame(remoteStream)
                CallFrameStyle.GLASS -> GlassVideoFrame(remoteStream)
                CallFrameStyle.RAINBOW -> RainbowVideoFrame(remoteStream)
            }
        }

        // 📱 PiP: Локальне відео (draggable + swipe to switch camera)
        if (!isFullscreen && localStream != null) {
            LocalVideoPiP(
                localStream = localStream,
                offset = pipOffset,
                onOffsetChange = { newOffset ->
                    pipOffset = newOffset
                },
                onSwitchCamera = onSwitchCamera
            )
        }
    }
}

/**
 * 📱 Picture-in-Picture для локального відео (draggable + swipe to switch camera)
 */
@Composable
fun LocalVideoPiP(
    localStream: MediaStream,
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onSwitchCamera: () -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset(x = offset.x.dp, y = offset.y.dp)
            .padding(16.dp)
            .width(120.dp)
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1a1a1a))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffset ->
                        isDragging = true
                        dragStartOffset = dragOffset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        // Перевірка на swipe (горизонтальний рух більше 100px)
                        val totalDragX = change.position.x - dragStartOffset.x
                        if (kotlin.math.abs(totalDragX) > 100f && kotlin.math.abs(dragAmount.y) < 50f) {
                            // Swipe left/right → switch camera
                            onSwitchCamera()
                            isDragging = false
                        } else {
                            // Normal drag → move PiP
                            onOffsetChange(
                                Offset(
                                    x = (offset.x + dragAmount.x).coerceIn(0f, 800f),
                                    y = (offset.y + dragAmount.y).coerceIn(0f, 1400f)
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                    }
                )
            }
    ) {
        // Локальне відео
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(true)
                    setEnableHardwareScaler(true)
                    if (localStream.videoTracks.isNotEmpty()) {
                        localStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Індикатор перемикання камери
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color(0x99000000), CircleShape)
                .clickable {
                    // TODO: Switch camera
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Рамка при перетягуванні
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF2196F3).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
    }
}

/**
 * 🎨 CLASSIC: Класична рамка з легкою тінню
 */
@Composable
fun ClassicVideoFrame(remoteStream: MediaStream) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        // WebRTC SurfaceViewRenderer
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    // Підключаємо відеотрек
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 💡 NEON: Неонова рамка з пульсуючим світінням
 */
@Composable
fun NeonVideoFrame(remoteStream: MediaStream) {
    var animatedAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            animatedAlpha = if (animatedAlpha == 1f) 0.5f else 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF00ffff).copy(alpha = animatedAlpha * 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 🌈 GRADIENT: Градієнтна рамка з кольоровим переходом
 */
@Composable
fun GradientVideoFrame(remoteStream: MediaStream) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFFf093fb)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * ⚪ MINIMAL: Мінімалістична без рамки
 */
@Composable
fun MinimalVideoFrame(remoteStream: MediaStream) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 💎 GLASS: Скляний ефект з blur
 */
@Composable
fun GlassVideoFrame(remoteStream: MediaStream) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 🌈 RAINBOW: Веселкова анімована рамка
 */
@Composable
fun RainbowVideoFrame(remoteStream: MediaStream) {
    var offsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(50)
            offsetX = (offsetX + 10f) % 360f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFff0000),
                        Color(0xFFff7f00),
                        Color(0xFFffff00),
                        Color(0xFF00ff00),
                        Color(0xFF0000ff),
                        Color(0xFF4b0082),
                        Color(0xFF9400d3)
                    ),
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 1000f, 1000f)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    setZOrderMediaOverlay(false)
                    setEnableHardwareScaler(true)
                    if (remoteStream.videoTracks.isNotEmpty()) {
                        remoteStream.videoTracks[0].addSink(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Екран помилки
 */
@Composable
fun ErrorScreen(error: String, viewModel: CallsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFd32f2f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Помилка дзвінка",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            fontSize = 14.sp,
            color = Color(0xFFbbbbbb),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.endCall() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd32f2f))
        ) {
            Text("Закрити")
        }
    }
}

/**
 * Екран без активного дзвінка
 */
@Composable
fun IdleScreen(viewModel: CallsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Calls",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Немає активних дзвінків",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Вспомогательні функції
 */
fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
