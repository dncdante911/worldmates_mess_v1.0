package com.worldmates.messenger.ui.messages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

/**
 * 🎬 Стилі рамок для відеоповідомлень
 */
enum class VideoMessageFrameStyle(val label: String, val emoji: String) {
    CIRCLE("Круглий", "⭕"),
    ROUNDED("Заокруглений", "🔲"),
    NEON("Неоновий", "💡"),
    GRADIENT("Градієнт", "🌈"),
    MINIMAL("Мінімальний", "⚪"),
    RAINBOW("Веселковий", "🌈")
}

/**
 * 📹 Отримати збережений стиль рамки відеоповідомлень
 */
fun getSavedVideoMessageFrameStyle(context: Context): VideoMessageFrameStyle {
    val prefs = context.getSharedPreferences("worldmates_prefs", Context.MODE_PRIVATE)
    val styleName = prefs.getString("video_message_frame_style", VideoMessageFrameStyle.CIRCLE.name)
    return try {
        VideoMessageFrameStyle.valueOf(styleName ?: VideoMessageFrameStyle.CIRCLE.name)
    } catch (e: Exception) {
        VideoMessageFrameStyle.CIRCLE
    }
}

/**
 * 📹 Зберегти стиль рамки відеоповідомлень
 */
fun saveVideoMessageFrameStyle(context: Context, style: VideoMessageFrameStyle) {
    val prefs = context.getSharedPreferences("worldmates_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("video_message_frame_style", style.name).apply()
}

/**
 * 🎬 Компонент для запису відеоповідомлень
 */
@Composable
fun VideoMessageRecorder(
    maxDurationSeconds: Int = 120,  // 2 хвилини за замовчуванням
    isPremiumUser: Boolean = false,  // Прему|м користувачі мають 5 хвилин
    onVideoRecorded: (File) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Максимальна тривалість залежить від статусу користувача
    val actualMaxDuration = if (isPremiumUser) 300 else maxDurationSeconds  // 5 хв для преміум

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var videoFile by remember { mutableStateOf<File?>(null) }

    // Таймер для відліку часу запису
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording && recordingDuration < actualMaxDuration) {
                delay(1000)
                recordingDuration++
                // Автоматично зупинити при досягненні ліміту
                if (recordingDuration >= actualMaxDuration) {
                    isRecording = false
                }
            }
        }
    }

    // Стиль рамки
    val frameStyle = remember { getSavedVideoMessageFrameStyle(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview з рамкою
        VideoMessageCameraPreview(
            isFrontCamera = isFrontCamera,
            isRecording = isRecording,
            frameStyle = frameStyle,
            modifier = Modifier.fillMaxSize()
        )

        // Верхня панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка закриття
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрити",
                    tint = Color.White
                )
            }

            // Таймер
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Пульсуюча точка запису
                        val infiniteTransition = rememberInfiniteTransition(label = "recording")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White.copy(alpha = alpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRecordingTime(recordingDuration),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${formatRecordingTime(actualMaxDuration)}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Кнопка перемикання камери
            IconButton(
                onClick = { isFrontCamera = !isFrontCamera },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Перемкнути камеру",
                    tint = Color.White
                )
            }
        }

        // Нижня панель з кнопкою запису
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Підказка
            if (!isRecording) {
                Text(
                    text = "Натисніть для запису (до ${actualMaxDuration / 60} хв)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Кнопка запису
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else Color.White)
                    .clickable {
                        if (isRecording) {
                            // Зупинити запис
                            isRecording = false
                            videoFile?.let { onVideoRecorded(it) }
                        } else {
                            // Почати запис
                            isRecording = true
                            videoFile = createVideoFile(context)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Показати квадрат "стоп"
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

/**
 * 🎥 Camera Preview для відеоповідомлень
 */
@Composable
fun VideoMessageCameraPreview(
    isFrontCamera: Boolean,
    isRecording: Boolean,
    frameStyle: VideoMessageFrameStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (e: Exception) {
                        Log.e("VideoMessageRecorder", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Накладення рамки
        VideoMessageFrameOverlay(
            style = frameStyle,
            isRecording = isRecording,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 🎨 Накладення рамки на відеоповідомлення
 */
@Composable
fun VideoMessageFrameOverlay(
    style: VideoMessageFrameStyle,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    when (style) {
        VideoMessageFrameStyle.CIRCLE -> {
            // Круглий стиль - маска що показує тільки круг
            Box(modifier = modifier) {
                // Тут буде маска для круглого вигляду
            }
        }
        VideoMessageFrameStyle.NEON -> {
            // Неонова рамка з пульсацією
            val infiniteTransition = rememberInfiniteTransition(label = "neon")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow"
            )
            Box(
                modifier = modifier
                    .padding(16.dp)
                    .border(
                        width = 3.dp,
                        color = Color(0xFF00FFFF).copy(alpha = glowAlpha),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
        VideoMessageFrameStyle.GRADIENT -> {
            Box(
                modifier = modifier
                    .padding(16.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2),
                                Color(0xFFf093fb)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
        VideoMessageFrameStyle.RAINBOW -> {
            var offsetX by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(50)
                    offsetX = (offsetX + 10f) % 360f
                }
            }
            Box(
                modifier = modifier
                    .padding(16.dp)
                    .border(
                        width = 3.dp,
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
                            end = Offset(offsetX + 500f, 500f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
        VideoMessageFrameStyle.ROUNDED -> {
            Box(
                modifier = modifier
                    .padding(16.dp)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
        VideoMessageFrameStyle.MINIMAL -> {
            // Без рамки
        }
    }

    // Індикатор запису
    if (isRecording) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 4.dp,
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = if (style == VideoMessageFrameStyle.CIRCLE) CircleShape else RoundedCornerShape(24.dp)
                )
        )
    }
}

/**
 * 📹 Bubble для відображення відеоповідомлення в чаті
 */
@Composable
fun VideoMessageBubble(
    videoUrl: String,
    duration: Int,  // в секундах
    frameStyle: VideoMessageFrameStyle,
    isFromMe: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = when (frameStyle) {
        VideoMessageFrameStyle.CIRCLE -> CircleShape
        else -> RoundedCornerShape(16.dp)
    }

    Box(
        modifier = modifier
            .size(if (frameStyle == VideoMessageFrameStyle.CIRCLE) 200.dp else 220.dp)
            .clip(shape)
            .background(Color.Black)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail відео (placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        )

        // Кнопка play
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = "Відтворити",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(48.dp)
        )

        // Тривалість
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = formatRecordingTime(duration),
                color = Color.White,
                fontSize = 12.sp
            )
        }

        // Рамка
        when (frameStyle) {
            VideoMessageFrameStyle.NEON -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color(0xFF00FFFF), shape)
                )
            }
            VideoMessageFrameStyle.GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF667eea), Color(0xFF764ba2), Color(0xFFf093fb))
                            ),
                            shape
                        )
                )
            }
            else -> {}
        }
    }
}

/**
 * 📹 Селектор стилю рамки відеоповідомлень
 */
@Composable
fun VideoMessageFrameSelector(
    currentStyle: VideoMessageFrameStyle,
    onStyleSelected: (VideoMessageFrameStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎬 Стиль відеоповідомлень",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                VideoMessageFrameStyle.entries.forEach { style ->
                    val isSelected = style == currentStyle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable {
                                saveVideoMessageFrameStyle(context, style)
                                onStyleSelected(style)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.emoji,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = style.label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFbbbbbb)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions

private fun formatRecordingTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

private fun createVideoFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.cacheDir
    return File.createTempFile("VIDEO_${timestamp}_", ".mp4", storageDir)
}
