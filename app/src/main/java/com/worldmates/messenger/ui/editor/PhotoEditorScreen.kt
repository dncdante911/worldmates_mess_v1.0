package com.worldmates.messenger.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

/**
 * 🎨 PHOTO EDITOR SCREEN
 *
 * Полнофункциональный редактор фото с:
 * - Обрезка (crop)
 * - Поворот (rotate)
 * - Фильтры (filters)
 * - Рисование (draw)
 * - Текст (text)
 * - Стикеры (stickers)
 * - Яркость/Контраст/Насыщенность
 * - Сохранение и отправка
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUrl: String,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit,
    viewModel: PhotoEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTool by viewModel.currentTool.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val contrast by viewModel.contrast.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val rotationAngle by viewModel.rotationAngle.collectAsState()
    val drawingPaths by viewModel.drawingPaths.collectAsState()
    val textElements by viewModel.textElements.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()

    LaunchedEffect(imageUrl) {
        viewModel.loadImage(imageUrl, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор фото", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрыть")
                    }
                },
                actions = {
                    // Undo button
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo()
                    ) {
                        Icon(Icons.Default.Undo, "Отменить")
                    }

                    // Redo button
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo()
                    ) {
                        Icon(Icons.Default.Redo, "Вернуть")
                    }

                    // Save button
                    IconButton(
                        onClick = {
                            val savedFile = viewModel.saveImage(context)
                            if (savedFile != null) {
                                onSave(savedFile)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Ошибка сохранения",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, "Сохранить")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Tool-specific controls
                when (currentTool) {
                    EditorTool.DRAW -> DrawControls(
                        selectedColor = selectedColor,
                        brushSize = brushSize,
                        onColorChange = { viewModel.setDrawColor(it) },
                        onBrushSizeChange = { viewModel.setBrushSize(it) }
                    )
                    EditorTool.FILTER -> FilterControls(
                        selectedFilter = selectedFilter,
                        onFilterChange = { viewModel.applyFilter(it) }
                    )
                    EditorTool.ADJUST -> AdjustmentControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        onBrightnessChange = { viewModel.setBrightness(it) },
                        onContrastChange = { viewModel.setContrast(it) },
                        onSaturationChange = { viewModel.setSaturation(it) }
                    )
                    EditorTool.ROTATE -> RotateControls(
                        rotationAngle = rotationAngle,
                        onRotate = { viewModel.rotate(it) }
                    )
                    EditorTool.TEXT -> TextControls(
                        onAddText = { text, color, size ->
                            viewModel.addText(text, color, size)
                        }
                    )
                    EditorTool.STICKER -> StickerControls(
                        onAddSticker = { sticker ->
                            viewModel.addSticker(sticker)
                        }
                    )
                    EditorTool.CROP -> CropControls(
                        onCrop = { viewModel.applyCrop() }
                    )
                }

                Divider()

                // Tool selector
                ToolSelector(
                    currentTool = currentTool,
                    onToolSelected = { viewModel.selectTool(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Canvas for editing
            PhotoEditorCanvas(
                viewModel = viewModel,
                currentTool = currentTool,
                onDrawPath = { path -> viewModel.addDrawPath(path) },
                onAddText = { text, x, y -> viewModel.addTextAt(text, x, y) }
            )
        }
    }
}

/**
 * 🎨 Photo Editor Canvas
 */
@Composable
private fun PhotoEditorCanvas(
    viewModel: PhotoEditorViewModel,
    currentTool: EditorTool,
    onDrawPath: (Path) -> Unit,
    onAddText: (String, Float, Float) -> Unit
) {
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Display image
        editedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Edited photo",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool) {
                        when (currentTool) {
                            EditorTool.DRAW -> {
                                detectDragGestures { change, dragAmount ->
                                    // Drawing logic handled in ViewModel
                                    change.consume()
                                }
                            }
                            EditorTool.TEXT -> {
                                detectTapGestures { offset ->
                                    // Show text input dialog
                                }
                            }
                            else -> {}
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * 🛠️ Tool Selector
 */
@Composable
private fun ToolSelector(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(EditorTool.values()) { tool ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onToolSelected(tool) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (currentTool == tool) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.displayName,
                            tint = if (currentTool == tool) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.displayName,
                    fontSize = 11.sp,
                    color = if (currentTool == tool) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

/**
 * ✏️ Draw Controls
 */
@Composable
private fun DrawControls(
    selectedColor: Color,
    brushSize: Float,
    onColorChange: (Color) -> Unit,
    onBrushSizeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Цвет и размер кисти", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Color picker
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drawingColors) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (color == selectedColor) 3.dp else 1.dp,
                            color = if (color == selectedColor) Color.White else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Brush size slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Размер:", fontSize = 14.sp)
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 2f..50f,
                modifier = Modifier.weight(1f)
            )
            Text("${brushSize.toInt()}px", fontSize = 14.sp)
        }
    }
}

/**
 * 🎭 Filter Controls
 */
@Composable
private fun FilterControls(
    selectedFilter: PhotoFilter,
    onFilterChange: (PhotoFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PhotoFilter.values()) { filter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onFilterChange(filter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedFilter == filter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = if (selectedFilter == filter) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.icon,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = filter.displayName,
                    fontSize = 11.sp,
                    color = if (selectedFilter == filter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

/**
 * 🎚️ Adjustment Controls
 */
@Composable
private fun AdjustmentControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Настройка изображения", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Brightness
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("☀️ Яркость:", fontSize = 14.sp, modifier = Modifier.width(100.dp))
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = -1f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(brightness * 100).toInt()}%", fontSize = 12.sp, modifier = Modifier.width(50.dp))
        }

        // Contrast
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔲 Контраст:", fontSize = 14.sp, modifier = Modifier.width(100.dp))
            Slider(
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f)
            )
            Text("${(contrast * 100).toInt()}%", fontSize = 12.sp, modifier = Modifier.width(50.dp))
        }

        // Saturation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎨 Насыщен:", fontSize = 14.sp, modifier = Modifier.width(100.dp))
            Slider(
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f)
            )
            Text("${(saturation * 100).toInt()}%", fontSize = 12.sp, modifier = Modifier.width(50.dp))
        }
    }
}

/**
 * 🔄 Rotate Controls
 */
@Composable
private fun RotateControls(
    rotationAngle: Int,
    onRotate: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { onRotate(-90) }) {
            Icon(Icons.Default.RotateLeft, "Влево 90°")
            Spacer(modifier = Modifier.width(4.dp))
            Text("90° влево")
        }

        Button(onClick = { onRotate(180) }) {
            Text("180°")
        }

        Button(onClick = { onRotate(90) }) {
            Icon(Icons.Default.RotateRight, "Вправо 90°")
            Spacer(modifier = Modifier.width(4.dp))
            Text("90° вправо")
        }
    }
}

/**
 * 📝 Text Controls
 */
@Composable
private fun TextControls(
    onAddText: (String, Color, Float) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Добавить текст", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Введите текст...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Color picker
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(textColors) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (color == selectedColor) 3.dp else 1.dp,
                            color = if (color == selectedColor) Color.White else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = color }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onAddText(textInput, selectedColor, 24f)
                    textInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить текст")
        }
    }
}

/**
 * 😊 Sticker Controls
 */
@Composable
private fun StickerControls(
    onAddSticker: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(availableStickers) { sticker ->
            Text(
                text = sticker,
                fontSize = 40.sp,
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onAddSticker(sticker) }
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}

/**
 * ✂️ Crop Controls
 */
@Composable
private fun CropControls(
    onCrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(onClick = { /* Reset crop */ }) {
            Text("Сбросить")
        }

        Button(onClick = onCrop) {
            Icon(Icons.Default.Crop, "Обрезать")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Применить обрезку")
        }
    }
}

// 🎨 Color palettes
private val drawingColors = listOf(
    Color.Black, Color.White, Color.Red, Color.Blue,
    Color.Green, Color.Yellow, Color.Magenta, Color.Cyan
)

private val textColors = listOf(
    Color.White, Color.Black, Color.Red, Color.Blue,
    Color.Green, Color.Yellow
)

// 😊 Available stickers
private val availableStickers = listOf(
    "😀", "😂", "😍", "😎", "🥳", "😢", "😡", "🤔",
    "👍", "👎", "❤️", "💔", "🔥", "⭐", "🎉", "🎈"
)

/**
 * 🛠️ Editor Tools
 */
enum class EditorTool(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    DRAW("Рисовать", Icons.Default.Brush),
    FILTER("Фильтры", Icons.Default.FilterVintage),
    ADJUST("Настройки", Icons.Default.Tune),
    ROTATE("Поворот", Icons.Default.RotateRight),
    TEXT("Текст", Icons.Default.TextFields),
    STICKER("Стикеры", Icons.Default.EmojiEmotions),
    CROP("Обрезка", Icons.Default.Crop)
}

/**
 * 🎭 Photo Filters
 */
enum class PhotoFilter(
    val displayName: String,
    val icon: String
) {
    NONE("Нет", "🖼️"),
    GRAYSCALE("Ч/Б", "⚫"),
    SEPIA("Сепия", "🟤"),
    INVERT("Негатив", "🔄"),
    BLUR("Размытие", "🌫️"),
    SHARPEN("Резкость", "🔪")
}
