package com.worldmates.messenger.network

import android.content.Context
import android.util.Log
import com.worldmates.messenger.data.Constants
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
 * Менеджер для загрузки медиа-файлів на сервер
 */
class MediaUploader(private val context: Context) {

    sealed class UploadResult {
        data class Success(val mediaId: String, val url: String, val thumbnail: String? = null) : UploadResult()
        data class Error(val message: String, val exception: Exception? = null) : UploadResult()
        data class Progress(val percent: Int) : UploadResult()
    }

    companion object {
        private const val TAG = "MediaUploader"
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

            // Проверяем существование файла
            if (!file.exists()) {
                return@withContext UploadResult.Error("Файл не знайдено: $filePath")
            }

            // Проверяем размер файла
            if (!validateFileSize(file, mediaType, isPremium)) {
                return@withContext UploadResult.Error("Файл занадто великий для типу: $mediaType")
            }

            // Создаем RequestBody с прогрессом
            val requestBody = ProgressRequestBody(file, getMimeType(mediaType), onProgress)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )

            val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaType())
            val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.uploadMedia(
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
                        UploadResult.Error("Невідповідь від серверу: mediaId або url null")
                    }
                }
                400 -> UploadResult.Error(response.errorMessage ?: "Помилка запиту")
                401 -> UploadResult.Error("Не авторизовано")
                413 -> UploadResult.Error("Файл занадто великий")
                else -> UploadResult.Error(response.errorMessage ?: "Помилка ${response.apiStatus}")
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

    /**
     * Загружает аватар группы
     */
    suspend fun uploadGroupAvatar(
        accessToken: String,
        groupId: Long,
        filePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)

            if (!file.exists()) {
                return@withContext UploadResult.Error("Файл не знайдено")
            }

            val requestBody = ProgressRequestBody(file, "image/*", onProgress)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )

            val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaType())
            val groupIdBody = groupId.toString().toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.uploadGroupAvatar(
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

    /**
     * Загружает аватар пользователя
     */
    suspend fun uploadUserAvatar(
        accessToken: String,
        filePath: String,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)

            if (!file.exists()) {
                return@withContext UploadResult.Error("Файл не знайдено")
            }

            val requestBody = ProgressRequestBody(file, "image/*", onProgress)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )

            val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.uploadUserAvatar(
                accessToken = accessTokenBody,
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
            Log.e(TAG, "Error uploading user avatar: ${e.message}", e)
            UploadResult.Error("Помилка: ${e.message}", e)
        }
    }

    private fun validateFileSize(file: File, mediaType: String, isPremium: Boolean): Boolean {
        val fileSize = file.length()
        val maxSize = when (mediaType) {
            Constants.MESSAGE_TYPE_IMAGE -> Constants.MAX_IMAGE_SIZE
            Constants.MESSAGE_TYPE_VIDEO -> Constants.MAX_VIDEO_SIZE
            Constants.MESSAGE_TYPE_AUDIO -> Constants.MAX_AUDIO_SIZE
            Constants.MESSAGE_TYPE_VOICE -> Constants.MAX_AUDIO_SIZE
            Constants.MESSAGE_TYPE_FILE -> if (isPremium) Constants.MAX_FILE_SIZE_PREMIUM else Constants.MAX_FILE_SIZE_REGULAR
            else -> Constants.MAX_FILE_SIZE_REGULAR
        }
        return fileSize <= maxSize
    }

    private fun getMimeType(mediaType: String): String {
        return when (mediaType) {
            Constants.MESSAGE_TYPE_IMAGE -> "image/*"
            Constants.MESSAGE_TYPE_VIDEO -> "video/*"
            Constants.MESSAGE_TYPE_AUDIO -> "audio/*"
            Constants.MESSAGE_TYPE_VOICE -> "audio/*"
            Constants.MESSAGE_TYPE_FILE -> "application/*"
            else -> "application/octet-stream"
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
 * Утилита для роботи с файловой системой
 */
class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
    }

    /**
     * Копирует файл из URI в локальный кэш
     */
    fun copyUriToCache(uri: android.net.Uri, fileName: String? = null): File? {
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
    fun getDisplayName(uri: android.net.Uri): String {
        return when {
            uri.scheme == "content" -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    if (it.moveToFirst()) {
                        it.getString(nameIndex)
                    } else {
                        "unknown_${System.currentTimeMillis()}"
                    }
                } ?: "unknown_${System.currentTimeMillis()}"
            }
            else -> uri.lastPathSegment ?: "unknown_${System.currentTimeMillis()}"
        }
    }

    /**
     * Получает MIME тип из URI
     */
    fun getMimeType(uri: android.net.Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Получает размер файла из URI
     */
    fun getFileSize(uri: android.net.Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            val sizeIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
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
            if (file.isFile && (file.name.startsWith("VOICE_") || file.name.startsWith("IMG_") || file.name.startsWith("VID_"))) {
                deletedSize += file.length()
                file.delete()
            }
        }
        return deletedSize
    }
}