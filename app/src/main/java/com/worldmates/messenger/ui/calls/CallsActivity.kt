package com.worldmates.messenger.ui.calls

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer

class CallsActivity : ComponentActivity() {

    private lateinit var callsViewModel: CallsViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false) &&
                permissions.getOrDefault(Manifest.permission.CAMERA, false)) {
                // Дозволи отримано
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callsViewModel = ViewModelProvider(this).get(CallsViewModel::class.java)

        // Запросити дозволи
        requestPermissions()

        setContent {
            CallsScreen(callsViewModel, this)
        }
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
fun CallsScreen(viewModel: CallsViewModel, activity: CallsActivity) {
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
                IncomingCallScreen(incomingCall!!, viewModel)
            }
            callConnected -> {
                ActiveCallScreen(
                    viewModel = viewModel,
                    remoteStream = remoteStream,
                    connectionState = connectionState ?: "CONNECTING"
                )
            }
            callError != null -> {
                ErrorScreen(callError!!, viewModel)
            }
            else -> {
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
 * Екран активного дзвінка
 */
@Composable
fun ActiveCallScreen(
    viewModel: CallsViewModel,
    remoteStream: MediaStream?,
    connectionState: String
) {
    var audioEnabled by remember { mutableStateOf(true) }
    var videoEnabled by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }

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
                // Показати відео
                RemoteVideoView(remoteStream)
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

        // Топ: інформація про дзвінок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
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
 * Компонент для відображення віддаленої відео потоку
 */
@Composable
fun RemoteVideoView(remoteStream: MediaStream) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Відеопотік (WebRTC)",
            color = Color.White,
            fontSize = 16.sp
        )
        // TODO: Інтегрувати SurfaceViewRenderer
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
