package com.worldmates.messenger.network

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Менеджер для загрузки медиа-файлов на сервер
 */
class MediaUploader(
    private val context: Context,
    private val apiService: WorldMatesApi
) {

    sealed class UploadResult {
        data class Success(val mediaId: String, val url: String, val thumbnail: String? = null) : UploadResult()
        data class Error(val message: String, val exception: Exception? = null) : UploadResult()
        data class Progress(val percent: Int) : UploadResult()
    }

    companion object {
        private const val TAG = "MediaUploader"
        private const val MAX_FILE_SIZE_REGULAR = 2 * 1024 * 1024 * 1024L // 2GB
        private const val MAX_FILE_SIZE_PREMIUM = 10 * 1024 * 1024 * 1024L // 10GB
        private const val MAX_IMAGE_SIZE = 50 * 1024 * 1024L // 50MB
        private const val MAX_VIDEO_SIZE = 5 * 1024 * 1024 * 1024L // 5GB
        private const val MAX_AUDIO_SIZE = 1 * 1024 * 1024 * 1024L // 1GB
    }

    /**
     * Загружает медиа-файл на сервер
     */
    suspend fun uploadMedia(
        accessToken: String,
        mediaType: String,
        filePath: String,
        recipientId: Long? = null,
        groupId: Long? = null,
        isPremium: Boolean = false,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)

            // Проверяем размер файла
            if (!validateFileSize(file, mediaType, isPremium)) {
                return@withContext UploadResult.Error("Файл занадто великий")
            }

            // Создаем RequestBody с прогрессом
            val requestBody = ProgressRequestBody(file, mediaType, onProgress)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )

            val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaType())
            val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaType())

            val response = apiService.uploadMedia(
                accessToken = accessTokenBody,
                mediaType = mediaTypeBody,
                recipientId = recipientId?.toString()?.toRequestBody("text/plain".toMediaType()),
                groupId = groupId?.toString()?.toRequestBody("text/plain".toMediaType()),
                file = filePart
            )

            when (response.apiStatus) {
                200 -> {
                    if (response.mediaId != null && response.url != null) {
                        Log.d(TAG, "Медіа завантажено: ${response.url}")
                        UploadResult.Success(response.mediaId, response.url, response.thumbnail)
                    } else {
                        UploadResult.Error("Невідповідь від серверу")
                    }
                }
                400, 401 -> UploadResult.Error(response.errorMessage ?: "Помилка авторизації")
                else -> UploadResult.Error(response.errorMessage ?: "Невідома помилка")
            }

        } catch (e: IOException) {
            Log.e(TAG, "IO Exception: ${e.message}", e)
            UploadResult.Error("Помилка мережі: ${e.message}", e)
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Exception: ${e.code()}", e)
            UploadResult.Error("HTTP ${e.code()}: ${e.message()}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            UploadResult.Error("Помилка: ${e.message}", e)
        }
    }

    private fun validateFileSize(file: File, mediaType: String, isPremium: Boolean): Boolean {
        val fileSize = file.length()
        val maxSize = when (mediaType) {
            "image" -> MAX_IMAGE_SIZE
            "video" -> MAX_VIDEO_SIZE
            "audio" -> MAX_AUDIO_SIZE
            "voice" -> MAX_AUDIO_SIZE
            "file" -> if (isPremium) MAX_FILE_SIZE_PREMIUM else MAX_FILE_SIZE_REGULAR
            else -> MAX_FILE_SIZE_REGULAR
        }
        return fileSize <= maxSize
    }

    /**
     * Загружает аватар группы
     */
    suspend fun uploadGroupAvatar(
        accessToken: String,
        groupId: Long,
        filePath: String
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaType())
            )

            val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaType())
            val groupIdBody = groupId.toString().toRequestBody("text/plain".toMediaType())

            val response = apiService.uploadGroupAvatar(
                accessToken = accessTokenBody,
                groupId = groupIdBody,
                file = filePart
            )

            when (response.apiStatus) {
                200 -> {
                    if (response.url != null) {
                        UploadResult.Success(response.mediaId ?: "", response.url)
                    } else {
                        UploadResult.Error("Невідповідь від серверу")
                    }
                }
                else -> UploadResult.Error(response.errorMessage ?: "Помилка завантаження")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading avatar: ${e.message}", e)
            UploadResult.Error("Помилка: ${e.message}", e)
        }
    }
}

/**
 * RequestBody з прогресом завантаження
 */
private class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: ((Int) -> Unit)?
) : RequestBody() {

    override fun contentType() = contentType.toMediaType()

    override fun contentLength() = file.length()

    override fun writeTo(sink: okio.BufferedSink) {
        val fileInputStream = file.inputStream()
        val totalBytes = file.length()
        var uploadedBytes = 0L

        fileInputStream.use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                uploadedBytes += read
                sink.write(buffer, 0, read)
                val progress = (uploadedBytes * 100 / totalBytes).toInt()
                onProgress?.invoke(progress)
            }
        }
    }
}

/**
 * Утилита для роботи з файловой системой
 */
class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
    }

    /**
     * Копирует файл из URI в локальный кэш
     */
    fun copyUriToCache(uri: Uri, fileName: String? = null): File? {
        return try {
            val displayName = fileName ?: getDisplayName(uri)
            val cacheFile = File(context.cacheDir, displayName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to cache: ${e.message}", e)
            null
        }
    }

    /**
     * Получает имя файла из URI
     */
    fun getDisplayName(uri: Uri): String {
        return when {
            uri.scheme == "content" -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (it.moveToFirst()) {
                        it.getString(nameIndex)
                    } else {
                        "unknown"
                    }
                } ?: "unknown"
            }
            else -> uri.lastPathSegment ?: "unknown"
        }
    }

    /**
     * Получает MIME тип из URI
     */
    fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Получает размер файла из URI
     */
    fun getFileSize(uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
            if (it.moveToFirst()) {
                it.getLong(sizeIndex)
            } else {
                0
            }
        } ?: 0
    }

    /**
     * Создает файл для записи голосового сообщения
     */
    fun createVoiceRecordingFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "VOICE_$timeStamp.m4a"
        return File(context.cacheDir, fileName)
    }

    /**
     * Получает расширение файла на основе MIME типа
     */
    fun getExtensionForMimeType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> {
                when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                    mimeType.contains("png") -> ".png"
                    mimeType.contains("webp") -> ".webp"
                    else -> ".jpg"
                }
            }
            mimeType.startsWith("video/") -> {
                when {
                    mimeType.contains("mp4") -> ".mp4"
                    mimeType.contains("quicktime") -> ".mov"
                    else -> ".mp4"
                }
            }
            mimeType.startsWith("audio/") -> {
                when {
                    mimeType.contains("mpeg") -> ".mp3"
                    mimeType.contains("wav") -> ".wav"
                    mimeType.contains("ogg") -> ".ogg"
                    mimeType.contains("aac") -> ".aac"
                    else -> ".m4a"
                }
            }
            else -> ""
        }
    }

    /**
     * Удаляет файл
     */
    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}", e)
            false
        }
    }

    /**
     * Очищает кэш медиа файлов
     */
    fun clearMediaCache(): Long {
        var deletedSize = 0L
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.startsWith("VOICE_") || file.name.startsWith("IMG_"))) {
                deletedSize += file.length()
                file.delete()
            }
        }
        return deletedSize
    }
}