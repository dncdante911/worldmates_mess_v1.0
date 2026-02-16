package com.worldmates.messenger.ui.editor

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 🎨 PHOTO EDITOR VIEWMODEL
 *
 * Manages photo editing state and operations:
 * - Image loading and manipulation
 * - Drawing paths
 * - Text elements
 * - Filters and adjustments
 * - Undo/Redo stack
 */
class PhotoEditorViewModel : ViewModel() {
    private val TAG = "PhotoEditorViewModel"

    // Image state
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    // Tool state
    private val _currentTool = MutableStateFlow(EditorTool.DRAW)
    val currentTool: StateFlow<EditorTool> = _currentTool.asStateFlow()

    // Drawing state
    private val _drawingPaths = MutableStateFlow<List<DrawPath>>(emptyList())
    val drawingPaths: StateFlow<List<DrawPath>> = _drawingPaths.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color.Red)
    val selectedColor: StateFlow<Color> = _selectedColor.asStateFlow()

    private val _brushSize = MutableStateFlow(10f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    // Text state
    private val _textElements = MutableStateFlow<List<TextElement>>(emptyList())
    val textElements: StateFlow<List<TextElement>> = _textElements.asStateFlow()

    // Filter state
    private val _selectedFilter = MutableStateFlow(PhotoFilter.NONE)
    val selectedFilter: StateFlow<PhotoFilter> = _selectedFilter.asStateFlow()

    // Adjustment state
    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(1f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    // Rotation state
    private val _rotationAngle = MutableStateFlow(0)
    val rotationAngle: StateFlow<Int> = _rotationAngle.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = mutableListOf<EditorState>()
    private val redoStack = mutableListOf<EditorState>()

    /**
     * Load image from URL or file path
     */
    fun loadImage(imageUrl: String, context: Context) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading image: $imageUrl")

                val bitmap = if (imageUrl.startsWith("http")) {
                    // Load from URL
                    val url = URL(imageUrl)
                    BitmapFactory.decodeStream(url.openStream())
                } else {
                    // Load from file
                    BitmapFactory.decodeFile(imageUrl)
                }

                if (bitmap != null) {
                    _originalBitmap.value = bitmap
                    _editedBitmap.value = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    Log.d(TAG, "✅ Image loaded: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.e(TAG, "❌ Failed to load bitmap")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading image: ${e.message}", e)
            }
        }
    }

    /**
     * Select editing tool
     */
    fun selectTool(tool: EditorTool) {
        _currentTool.value = tool
        Log.d(TAG, "Selected tool: ${tool.displayName}")
    }

    /**
     * Add drawing path
     */
    fun addDrawPath(path: Path) {
        saveStateForUndo()
        val drawPath = DrawPath(path, _selectedColor.value, _brushSize.value)
        _drawingPaths.value = _drawingPaths.value + drawPath
        applyDrawing()
    }

    /**
     * Set draw color
     */
    fun setDrawColor(color: Color) {
        _selectedColor.value = color
    }

    /**
     * Set brush size
     */
    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    /**
     * Add text element
     */
    fun addText(text: String, color: Color, size: Float) {
        saveStateForUndo()
        val textElement = TextElement(text, color, size, 0.5f, 0.5f)
        _textElements.value = _textElements.value + textElement
        applyText()
    }

    /**
     * Add text at specific position
     */
    fun addTextAt(text: String, x: Float, y: Float) {
        saveStateForUndo()
        val textElement = TextElement(text, _selectedColor.value, 24f, x, y)
        _textElements.value = _textElements.value + textElement
        applyText()
    }

    /**
     * Add sticker
     */
    fun addSticker(sticker: String) {
        saveStateForUndo()
        // TODO: Implement sticker overlay
        Log.d(TAG, "Adding sticker: $sticker")
    }

    /**
     * Apply filter
     */
    fun applyFilter(filter: PhotoFilter) {
        saveStateForUndo()
        _selectedFilter.value = filter

        val original = _originalBitmap.value ?: return
        val filtered = original.copy(Bitmap.Config.ARGB_8888, true)

        when (filter) {
            PhotoFilter.NONE -> {
                _editedBitmap.value = filtered
            }
            PhotoFilter.GRAYSCALE -> {
                _editedBitmap.value = applyGrayscale(filtered)
            }
            PhotoFilter.SEPIA -> {
                _editedBitmap.value = applySepia(filtered)
            }
            PhotoFilter.INVERT -> {
                _editedBitmap.value = applyInvert(filtered)
            }
            PhotoFilter.BLUR -> {
                _editedBitmap.value = applyBlur(filtered)
            }
            PhotoFilter.SHARPEN -> {
                _editedBitmap.value = applySharpen(filtered)
            }
        }

        Log.d(TAG, "Applied filter: ${filter.displayName}")
    }

    /**
     * Set brightness
     */
    fun setBrightness(value: Float) {
        _brightness.value = value
        applyAdjustments()
    }

    /**
     * Set contrast
     */
    fun setContrast(value: Float) {
        _contrast.value = value
        applyAdjustments()
    }

    /**
     * Set saturation
     */
    fun setSaturation(value: Float) {
        _saturation.value = value
        applyAdjustments()
    }

    /**
     * Rotate image
     */
    fun rotate(degrees: Int) {
        saveStateForUndo()
        _rotationAngle.value = (_rotationAngle.value + degrees) % 360

        val current = _editedBitmap.value ?: return
        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }

        val rotated = Bitmap.createBitmap(
            current,
            0, 0,
            current.width, current.height,
            matrix,
            true
        )

        _editedBitmap.value = rotated
        Log.d(TAG, "Rotated ${degrees}° (total: ${_rotationAngle.value}°)")
    }

    /**
     * Apply crop
     */
    fun applyCrop() {
        saveStateForUndo()
        // TODO: Implement crop with selection rectangle
        Log.d(TAG, "Applying crop")
    }

    /**
     * Save image to file
     */
    fun saveImage(context: Context): File? {
        try {
            val bitmap = _editedBitmap.value ?: return null

            val outputDir = context.cacheDir
            val outputFile = File(outputDir, "edited_${System.currentTimeMillis()}.jpg")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            Log.d(TAG, "✅ Image saved: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving image: ${e.message}", e)
            return null
        }
    }

    /**
     * Undo last operation
     */
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = captureCurrentState()
            redoStack.add(currentState)

            val previousState = undoStack.removeAt(undoStack.lastIndex)
            restoreState(previousState)

            Log.d(TAG, "⬅️ Undo (stack size: ${undoStack.size})")
        }
    }

    /**
     * Redo last undone operation
     */
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = captureCurrentState()
            undoStack.add(currentState)

            val nextState = redoStack.removeAt(redoStack.lastIndex)
            restoreState(nextState)

            Log.d(TAG, "➡️ Redo (stack size: ${redoStack.size})")
        }
    }

    /**
     * Check if can undo
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Check if can redo
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // Private helper methods

    private fun saveStateForUndo() {
        val state = captureCurrentState()
        undoStack.add(state)
        redoStack.clear()

        // Limit undo stack size
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    private fun captureCurrentState(): EditorState {
        val bitmap = _editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
        return EditorState(
            bitmap = bitmap,
            drawingPaths = _drawingPaths.value.toList(),
            textElements = _textElements.value.toList(),
            filter = _selectedFilter.value,
            brightness = _brightness.value,
            contrast = _contrast.value,
            saturation = _saturation.value,
            rotationAngle = _rotationAngle.value
        )
    }

    private fun restoreState(state: EditorState) {
        _editedBitmap.value = state.bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        _drawingPaths.value = state.drawingPaths
        _textElements.value = state.textElements
        _selectedFilter.value = state.filter
        _brightness.value = state.brightness
        _contrast.value = state.contrast
        _saturation.value = state.saturation
        _rotationAngle.value = state.rotationAngle
    }

    private fun applyDrawing() {
        val original = _originalBitmap.value ?: return
        val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        _drawingPaths.value.forEach { drawPath ->
            val paint = Paint().apply {
                color = drawPath.color.toArgb()
                strokeWidth = drawPath.strokeWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            // Convert Compose Path to Android Path
            val androidPath = android.graphics.Path()
            // TODO: Convert drawPath.path to androidPath

            canvas.drawPath(androidPath, paint)
        }

        _editedBitmap.value = bitmap
    }

    private fun applyText() {
        val current = _editedBitmap.value ?: return
        val bitmap = current.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        _textElements.value.forEach { element ->
            val paint = Paint().apply {
                color = element.color.toArgb()
                textSize = element.size * bitmap.width / 10  // Scale based on image width
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            val x = element.x * bitmap.width
            val y = element.y * bitmap.height

            canvas.drawText(element.text, x, y, paint)
        }

        _editedBitmap.value = bitmap
    }

    private fun applyAdjustments() {
        val original = _originalBitmap.value ?: return
        var adjusted = original.copy(Bitmap.Config.ARGB_8888, true)

        // Apply brightness, contrast, saturation
        val colorMatrix = ColorMatrix().apply {
            // Brightness
            postConcat(ColorMatrix().apply {
                val b = _brightness.value * 255
                set(floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                ))
            })

            // Contrast
            val c = _contrast.value
            val t = (1f - c) / 2f * 255
            postConcat(ColorMatrix().apply {
                set(floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                ))
            })

            // Saturation
            setSaturation(_saturation.value)
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        val result = Bitmap.createBitmap(adjusted.width, adjusted.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(adjusted, 0f, 0f, paint)

        _editedBitmap.value = result
    }

    // Filter implementations

    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applySepia(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyInvert(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyBlur(bitmap: Bitmap): Bitmap {
        // Simple box blur
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val rs = android.renderscript.RenderScript.create(null)
        // TODO: Implement RenderScript blur
        return result
    }

    private fun applySharpen(bitmap: Bitmap): Bitmap {
        // Sharpening filter
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        // TODO: Implement convolution kernel for sharpening
        return result
    }
}

/**
 * 📊 Editor State (for undo/redo)
 */
data class EditorState(
    val bitmap: Bitmap?,
    val drawingPaths: List<DrawPath>,
    val textElements: List<TextElement>,
    val filter: PhotoFilter,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val rotationAngle: Int
)

/**
 * ✏️ Drawing Path
 */
data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

/**
 * 📝 Text Element
 */
data class TextElement(
    val text: String,
    val color: Color,
    val size: Float,
    val x: Float,  // Normalized 0-1
    val y: Float   // Normalized 0-1
)
