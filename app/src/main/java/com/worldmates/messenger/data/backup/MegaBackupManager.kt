package com.worldmates.messenger.data.backup

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import nz.mega.sdk.*
import java.io.File
import kotlin.coroutines.resume

/**
 * 📦 MegaBackupManager - Управління бекапами в MEGA
 *
 * Функції:
 * - Авторизація в MEGA (email + password)
 * - Завантаження файлів на MEGA
 * - Скачування файлів з MEGA
 * - Список файлів бекапів
 * - Видалення файлів
 */
class MegaBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "MegaBackupManager"
        private const val BACKUP_FOLDER_NAME = "WorldMates Backups"

        // TODO: Замініть на ваш App Key з MEGA
        // Інструкція: https://mega.nz/sdk
        // 1. Зареєструйтеся на https://mega.nz
        // 2. Перейдіть на https://mega.nz/developers
        // 3. Створіть App і отримайте App Key
        private const val MEGA_APP_KEY = "YOUR_MEGA_APP_KEY"
    }

    private var megaApi: MegaApiAndroid? = null
    private var backupFolderNode: MegaNode? = null
    private var isInitialized = false

    // ==================== ІНІЦІАЛІЗАЦІЯ ====================

    /**
     * Ініціалізувати MEGA SDK
     */
    fun initialize() {
        try {
            megaApi = MegaApiAndroid(
                MEGA_APP_KEY,
                context.filesDir.absolutePath,
                object : MegaApiAndroid.MegaLogger {
                    override fun log(time: String?, loglevel: Int, source: String?, message: String?) {
                        when (loglevel) {
                            MegaApiAndroid.LOG_LEVEL_ERROR -> Log.e(TAG, "MEGA: $message")
                            MegaApiAndroid.LOG_LEVEL_WARNING -> Log.w(TAG, "MEGA: $message")
                            MegaApiAndroid.LOG_LEVEL_INFO -> Log.i(TAG, "MEGA: $message")
                            else -> Log.d(TAG, "MEGA: $message")
                        }
                    }
                }
            )
            isInitialized = true
            Log.d(TAG, "✅ MEGA SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize MEGA SDK: ${e.message}", e)
        }
    }

    // ==================== АВТОРИЗАЦІЯ ====================

    /**
     * Увійти в MEGA акаунт
     */
    suspend fun login(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || megaApi == null) {
            Log.e(TAG, "❌ MEGA SDK not initialized")
            return@withContext false
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "🔐 Logging in to MEGA: $email")

            megaApi?.login(email, password, object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
                    Log.d(TAG, "Login request started")
                }

                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                    if (error?.errorCode == MegaError.API_OK) {
                        Log.d(TAG, "✅ Login successful")

                        // Fetch nodes після успішного логіну
                        megaApi?.fetchNodes(object : MegaRequestListenerInterface {
                            override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}
                            override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                            override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                                if (error?.errorCode == MegaError.API_OK) {
                                    Log.d(TAG, "✅ Nodes fetched successfully")
                                    continuation.resume(true)
                                } else {
                                    Log.e(TAG, "❌ Failed to fetch nodes: ${error?.errorString}")
                                    continuation.resume(false)
                                }
                            }

                            override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {}
                        })
                    } else {
                        Log.e(TAG, "❌ Login failed: ${error?.errorString}")
                        continuation.resume(false)
                    }
                }

                override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                    Log.w(TAG, "⚠️ Login temporary error: ${error?.errorString}")
                }
            })

            continuation.invokeOnCancellation {
                Log.d(TAG, "Login cancelled")
            }
        }
    }

    /**
     * Перевірити чи користувач авторизований
     */
    fun isLoggedIn(): Boolean {
        val email = megaApi?.myEmail
        return !email.isNullOrEmpty()
    }

    /**
     * Вийти з MEGA акаунту
     */
    suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || megaApi == null) {
            return@withContext false
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "🚪 Logging out from MEGA")

            megaApi?.logout(object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}
                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                    if (error?.errorCode == MegaError.API_OK) {
                        Log.d(TAG, "✅ Logout successful")
                        backupFolderNode = null
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "❌ Logout failed: ${error?.errorString}")
                        continuation.resume(false)
                    }
                }

                override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {}
            })
        }
    }

    // ==================== УПРАВЛІННЯ ПАПКАМИ ====================

    /**
     * Отримати або створити папку для бекапів
     */
    private suspend fun getOrCreateBackupFolder(): MegaNode? = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Log.e(TAG, "❌ Not logged in")
            return@withContext null
        }

        try {
            // Спробувати знайти існуючу папку
            val rootNode = megaApi?.rootNode
            if (rootNode == null) {
                Log.e(TAG, "❌ Root node not found")
                return@withContext null
            }

            val children = megaApi?.getChildren(rootNode)
            val existingFolder = children?.find { it.name == BACKUP_FOLDER_NAME && it.isFolder }

            if (existingFolder != null) {
                Log.d(TAG, "📁 Found existing backup folder")
                backupFolderNode = existingFolder
                return@withContext existingFolder
            }

            // Створити нову папку
            suspendCancellableCoroutine { continuation ->
                Log.d(TAG, "📁 Creating new backup folder")

                megaApi?.createFolder(BACKUP_FOLDER_NAME, rootNode, object : MegaRequestListenerInterface {
                    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}
                    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                    override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                        if (error?.errorCode == MegaError.API_OK) {
                            val node = megaApi?.getNodeByHandle(request?.nodeHandle ?: 0)
                            Log.d(TAG, "✅ Backup folder created")
                            backupFolderNode = node
                            continuation.resume(node)
                        } else {
                            Log.e(TAG, "❌ Failed to create folder: ${error?.errorString}")
                            continuation.resume(null)
                        }
                    }

                    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {}
                })
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get/create backup folder: ${e.message}", e)
            null
        }
    }

    // ==================== ЗАВАНТАЖЕННЯ ФАЙЛІВ ====================

    /**
     * Завантажити файл на MEGA
     */
    suspend fun uploadFile(localFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Log.e(TAG, "❌ Not logged in")
            return@withContext false
        }

        try {
            val folder = backupFolderNode ?: getOrCreateBackupFolder()
            if (folder == null) {
                Log.e(TAG, "❌ Backup folder not found")
                return@withContext false
            }

            suspendCancellableCoroutine { continuation ->
                Log.d(TAG, "📤 Uploading file: ${localFile.name} (${localFile.length()} bytes)")

                megaApi?.startUpload(
                    localFile.absolutePath,
                    folder,
                    object : MegaTransferListenerInterface {
                        override fun onTransferStart(api: MegaApiJava?, transfer: MegaTransfer?) {
                            Log.d(TAG, "Upload started: ${transfer?.fileName}")
                        }

                        override fun onTransferUpdate(api: MegaApiJava?, transfer: MegaTransfer?) {
                            val progress = transfer?.transferredBytes?.toFloat()?.div(transfer.totalBytes) ?: 0f
                            Log.d(TAG, "Upload progress: ${(progress * 100).toInt()}%")
                        }

                        override fun onTransferFinish(api: MegaApiJava?, transfer: MegaTransfer?, error: MegaError?) {
                            if (error?.errorCode == MegaError.API_OK) {
                                Log.d(TAG, "✅ File uploaded successfully")
                                continuation.resume(true)
                            } else {
                                Log.e(TAG, "❌ Upload failed: ${error?.errorString}")
                                continuation.resume(false)
                            }
                        }

                        override fun onTransferTemporaryError(api: MegaApiJava?, transfer: MegaTransfer?, error: MegaError?) {
                            Log.w(TAG, "⚠️ Upload temporary error: ${error?.errorString}")
                        }

                        override fun onTransferData(api: MegaApiJava?, transfer: MegaTransfer?, buffer: ByteArray?): Boolean {
                            return true
                        }
                    }
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed: ${e.message}", e)
            false
        }
    }

    // ==================== СКАЧУВАННЯ ФАЙЛІВ ====================

    /**
     * Скачати файл з MEGA
     */
    suspend fun downloadFile(node: MegaNode, destinationPath: String): Boolean = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Log.e(TAG, "❌ Not logged in")
            return@withContext false
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "📥 Downloading file: ${node.name}")

            megaApi?.startDownload(
                node,
                destinationPath,
                object : MegaTransferListenerInterface {
                    override fun onTransferStart(api: MegaApiJava?, transfer: MegaTransfer?) {
                        Log.d(TAG, "Download started: ${transfer?.fileName}")
                    }

                    override fun onTransferUpdate(api: MegaApiJava?, transfer: MegaTransfer?) {
                        val progress = transfer?.transferredBytes?.toFloat()?.div(transfer.totalBytes) ?: 0f
                        Log.d(TAG, "Download progress: ${(progress * 100).toInt()}%")
                    }

                    override fun onTransferFinish(api: MegaApiJava?, transfer: MegaTransfer?, error: MegaError?) {
                        if (error?.errorCode == MegaError.API_OK) {
                            Log.d(TAG, "✅ File downloaded successfully")
                            continuation.resume(true)
                        } else {
                            Log.e(TAG, "❌ Download failed: ${error?.errorString}")
                            continuation.resume(false)
                        }
                    }

                    override fun onTransferTemporaryError(api: MegaApiJava?, transfer: MegaTransfer?, error: MegaError?) {
                        Log.w(TAG, "⚠️ Download temporary error: ${error?.errorString}")
                    }

                    override fun onTransferData(api: MegaApiJava?, transfer: MegaTransfer?, buffer: ByteArray?): Boolean {
                        return true
                    }
                }
            )
        }
    }

    // ==================== СПИСОК ФАЙЛІВ ====================

    /**
     * Отримати список всіх бекапів на MEGA
     */
    suspend fun listBackupFiles(): List<MegaBackupFile> = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Log.w(TAG, "⚠️ Not logged in")
            return@withContext emptyList()
        }

        try {
            val folder = backupFolderNode ?: getOrCreateBackupFolder()
            if (folder == null) {
                return@withContext emptyList()
            }

            val children = megaApi?.getChildren(folder) ?: return@withContext emptyList()

            val files = children
                .filter { !it.isFolder }
                .map { node ->
                    MegaBackupFile(
                        handle = node.handle,
                        name = node.name,
                        size = node.size,
                        createdTime = node.creationTime * 1000L,
                        modifiedTime = node.modificationTime * 1000L
                    )
                }
                .sortedByDescending { it.createdTime }

            Log.d(TAG, "✅ Found ${files.size} backup files")
            files

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to list backup files: ${e.message}", e)
            emptyList()
        }
    }

    // ==================== ВИДАЛЕННЯ ФАЙЛІВ ====================

    /**
     * Видалити файл з MEGA
     */
    suspend fun deleteFile(node: MegaNode): Boolean = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Log.e(TAG, "❌ Not logged in")
            return@withContext false
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "🗑️ Deleting file: ${node.name}")

            megaApi?.remove(node, object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}
                override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

                override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {
                    if (error?.errorCode == MegaError.API_OK) {
                        Log.d(TAG, "✅ File deleted successfully")
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "❌ Delete failed: ${error?.errorString}")
                        continuation.resume(false)
                    }
                }

                override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, error: MegaError?) {}
            })
        }
    }

    // ==================== ОТРИМАТИ ІНФОРМАЦІЮ ====================

    /**
     * Отримати email користувача
     */
    fun getUserEmail(): String? {
        return megaApi?.myEmail
    }

    /**
     * Отримати storage quota
     */
    fun getStorageQuota(): MegaStorageQuota? {
        if (!isLoggedIn()) {
            return null
        }

        val accountDetails = megaApi?.myAccountDetails ?: return null

        return MegaStorageQuota(
            totalBytes = accountDetails.storageMax,
            usedBytes = accountDetails.storageUsed,
            availableBytes = accountDetails.storageMax - accountDetails.storageUsed
        )
    }

    // ==================== CLEANUP ====================

    /**
     * Очистити ресурси
     */
    fun cleanup() {
        megaApi = null
        backupFolderNode = null
        isInitialized = false
        Log.d(TAG, "🧹 Cleaned up MEGA manager")
    }
}

/**
 * Інформація про файл на MEGA
 */
data class MegaBackupFile(
    val handle: Long,
    val name: String,
    val size: Long,
    val createdTime: Long,
    val modifiedTime: Long
)

/**
 * Інформація про storage quota в MEGA
 */
data class MegaStorageQuota(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
}
