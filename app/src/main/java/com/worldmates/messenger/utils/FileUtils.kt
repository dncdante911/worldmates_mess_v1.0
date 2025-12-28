package com.worldmates.messenger.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Утиліти для роботи з файлами
 */
object FileUtils {

    private const val TAG = "FileUtils"

    /**
     * Отримати File з Uri
     * Підтримує різні типи Uri (content://, file://)
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    // file:// URI - просто повертаємо File
                    File(uri.path ?: return null)
                }
                "content" -> {
                    // content:// URI - копіюємо в тимчасовий файл
                    copyUriToTempFile(context, uri)
                }
                else -> {
                    Log.e(TAG, "Unsupported URI scheme: ${uri.scheme}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file from URI: ${e.message}", e)
            null
        }
    }

    /**
     * Копіює вміст Uri в тимчасовий файл
     */
    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Отримуємо ім'я та розширення файлу
            val fileName = getFileName(context, uri) ?: "temp_${System.currentTimeMillis()}"
            val extension = getFileExtension(fileName)

            // Створюємо тимчасовий файл
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.$extension")

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            Log.d(TAG, "Файл скопійовано: ${tempFile.absolutePath}, розмір: ${tempFile.length()}")
            tempFile
        } catch (e: IOException) {
            Log.e(TAG, "Error copying URI to temp file", e)
            null
        }
    }

    /**
     * Отримати ім'я файлу з Uri
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * Отримати розширення файлу
     */
    fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) {
            fileName.substring(lastDot + 1)
        } else {
            "tmp"
        }
    }

    /**
     * Отримати розмір файлу з Uri
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            -1L
        }
    }

    /**
     * Отримати MIME тип файлу
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Перевірити, чи є Uri відео
     */
    fun isVideo(context: Context, uri: Uri): Boolean {
        val mimeType = getMimeType(context, uri)
        return mimeType?.startsWith("video/") == true
    }

    /**
     * Перевірити, чи є Uri зображення
     */
    fun isImage(context: Context, uri: Uri): Boolean {
        val mimeType = getMimeType(context, uri)
        return mimeType?.startsWith("image/") == true
    }

    /**
     * Видалити тимчасові файли старші за вказаний час
     */
    fun cleanupTempFiles(context: Context, olderThanMillis: Long = 24 * 60 * 60 * 1000) {
        try {
            val now = System.currentTimeMillis()
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") && now - file.lastModified() > olderThanMillis) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "Видалено старий тимчасовий файл: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files", e)
        }
    }
}
