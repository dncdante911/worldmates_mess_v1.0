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
     * Загружает несколько медиа-файлов на сервер (до 15 штук)
     */
    suspend fun uploadMultipleFiles(
        accessToken: String,
        files: List<File>,
        mediaTypes: List<String>,
        recipientId: Long? = null,
        groupId: Long? = null,
        onProgress: ((Int, Int) -> Unit)? = null // (current file index, progress)
    ): List<UploadResult> = withContext(Dispatchers.IO) {
        if (files.size > Constants.MAX_FILES_PER_MESSAGE) {
            return@withContext listOf(UploadResult.Error("Максимум ${Constants.MAX_FILES_PER_MESSAGE} файлів за раз"))
        }

        val results = mutableListOf<UploadResult>()
        files.forEachIndexed { index, file ->
            val mediaType = mediaTypes.getOrNull(index) ?: Constants.MESSAGE_TYPE_FILE
            Log.d(TAG, "Завантаження файлу ${index + 1}/${files.size}: ${file.name}")

            val result = uploadMedia(
                accessToken = accessToken,
                mediaType = mediaType,
                filePath = file.absolutePath,
                recipientId = recipientId,
                groupId = groupId,
                onProgress = { progress ->
                    onProgress?.invoke(index, progress)
                }
            )
            results.add(result)

            // Если загрузка не удалась, можно продолжить или прервать
            if (result is UploadResult.Error) {
                Log.e(TAG, "Помилка завантаження файлу ${file.name}: ${result.message}")
            }
        }
        results
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

            val mediaTypeBody = mediaType.toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.uploadMedia(
                accessToken = accessToken,
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

            val groupIdBody = groupId.toString().toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.uploadGroupAvatar(
                accessToken = accessToken,
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

            val response = RetrofitClient.apiService.uploadUserAvatar(
                accessToken = accessToken,
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

    private fun validateFileSize(file: File, mediaType: String, isPremium: Boolean = false): Boolean {
        val fileSize = file.length()
        val maxSize = when (mediaType) {
            Constants.MESSAGE_TYPE_IMAGE -> Constants.MAX_IMAGE_SIZE // 15MB
            Constants.MESSAGE_TYPE_VIDEO -> Constants.MAX_VIDEO_SIZE // 1GB (с сжатием)
            Constants.MESSAGE_TYPE_AUDIO, Constants.MESSAGE_TYPE_VOICE -> Constants.MAX_AUDIO_SIZE // 100MB
            Constants.MESSAGE_TYPE_FILE -> Constants.MAX_FILE_SIZE // 500MB для любых файлов
            else -> Constants.MAX_FILE_SIZE // 500MB по умолчанию
        }
        Log.d(TAG, "Валідація розміру: ${fileSize / 1024 / 1024}MB / ${maxSize / 1024 / 1024}MB для типу $mediaType")
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