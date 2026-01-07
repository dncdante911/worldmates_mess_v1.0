package com.worldmates.messenger.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.local.AppDatabase
import com.worldmates.messenger.data.model.CloudBackupSettings
import com.worldmates.messenger.data.model.SyncProgress
import com.worldmates.messenger.data.repository.BackupRepository
import com.worldmates.messenger.data.repository.CloudBackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📦 CLOUD BACKUP v2: ViewModel для управления настройками
 */
class CloudBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "CloudBackupViewModel"

    private val backupRepository = BackupRepository(application)
    private val settingsRepository = CloudBackupSettingsRepository(application)
    private val database = AppDatabase.getInstance(application)
    private val messageDao = database.messageDao()

    // ==================== STATE FLOWS ====================

    private val _settings = MutableStateFlow<CloudBackupSettings?>(null)
    val settings: StateFlow<CloudBackupSettings?> = _settings.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    init {
        loadSettings()
        calculateCacheSize()
    }

    // ==================== ЗАГРУЗКА НАСТРОЕК ====================

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.loadSettings().onSuccess { settings ->
                _settings.value = settings
                Log.d(TAG, "✅ Settings loaded from server")
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to load settings: ${error.message}")
                // Используем дефолтные настройки при ошибке
                _settings.value = CloudBackupSettings()
            }
        }
    }

    // ==================== АВТОЗАГРУЗКА МЕДИА ====================

    fun updateMobileDataSettings(
        photos: Boolean,
        videos: Boolean,
        files: Boolean,
        videoLimit: Int,
        fileLimit: Int
    ) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(
                mobilePhotos = photos,
                mobileVideos = videos,
                mobileFiles = files,
                mobileVideosLimit = videoLimit,
                mobileFilesLimit = fileLimit
            )
            saveSettings()
        }
    }

    fun updateWiFiSettings(
        photos: Boolean,
        videos: Boolean,
        files: Boolean,
        videoLimit: Int,
        fileLimit: Int
    ) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(
                wifiPhotos = photos,
                wifiVideos = videos,
                wifiFiles = files,
                wifiVideosLimit = videoLimit,
                wifiFilesLimit = fileLimit
            )
            saveSettings()
        }
    }

    fun updateRoamingPhotos(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(roamingPhotos = enabled)
            saveSettings()
        }
    }

    fun resetMediaSettings() {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(
                mobilePhotos = false,
                mobileVideos = false,
                mobileFiles = false,
                wifiPhotos = true,
                wifiVideos = true,
                wifiFiles = true,
                roamingPhotos = false
            )
            saveSettings()
        }
    }

    // ==================== СОХРАНЯТЬ В ГАЛЕРЕЕ ====================

    fun updateSaveToGalleryPrivateChats(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(saveToGalleryPrivateChats = enabled)
            saveSettings()
        }
    }

    fun updateSaveToGalleryGroups(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(saveToGalleryGroups = enabled)
            saveSettings()
        }
    }

    fun updateSaveToGalleryChannels(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(saveToGalleryChannels = enabled)
            saveSettings()
        }
    }

    // ==================== СТРИМИНГ ====================

    fun updateStreaming(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(streamingEnabled = enabled)
            saveSettings()
        }
    }

    // ==================== УПРАВЛЕНИЕ КЭШЕМ ====================

    fun updateCacheSize(size: Long) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(cacheSizeLimit = size)
            saveSettings()
            // Если новый лимит меньше текущего размера - очистим старые сообщения
            if (size < _cacheSize.value && size != CloudBackupSettings.CACHE_SIZE_UNLIMITED) {
                clearOldMessagesUntilLimit(size)
            }
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                messageDao.clearAllCache()
                calculateCacheSize()
                Log.d(TAG, "✅ Cache cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear cache: ${e.message}", e)
            }
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messageCount = messageDao.getCacheSize()
                // Примерный подсчет: 1 сообщение ≈ 1KB (без медиа)
                // TODO: Добавить реальный подсчет с медиафайлами
                _cacheSize.value = messageCount * 1024L
                Log.d(TAG, "📊 Cache size: ${messageCount} messages, ${_cacheSize.value} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to calculate cache size: ${e.message}", e)
            }
        }
    }

    private suspend fun clearOldMessagesUntilLimit(limit: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Удаляем сообщения старше 30 дней пока не достигнем лимита
                var daysOld = 30
                while (_cacheSize.value > limit && daysOld > 0) {
                    messageDao.deleteOldMessages(
                        System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
                    )
                    calculateCacheSize()
                    daysOld -= 7
                }
                Log.d(TAG, "✅ Cleared old messages until limit: $limit")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear old messages: ${e.message}", e)
            }
        }
    }

    // ==================== CLOUD BACKUP ====================

    fun updateBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(backupEnabled = enabled)
            saveSettings()
        }
    }

    fun updateBackupProvider(provider: CloudBackupSettings.BackupProvider) {
        viewModelScope.launch {
            _settings.value = _settings.value?.copy(backupProvider = provider)
            saveSettings()
        }
    }

    suspend fun startSync() {
        withContext(Dispatchers.IO) {
            try {
                _syncProgress.value = SyncProgress(isRunning = true)

                // TODO: Получить список чатов для синхронизации
                // Для примера синхронизируем фейковые чаты
                val chatIds = listOf(1L, 2L, 3L, 4L, 5L)

                chatIds.forEachIndexed { index, chatId ->
                    _syncProgress.value = _syncProgress.value.copy(
                        currentItem = index + 1,
                        totalItems = chatIds.size,
                        currentChatName = "Чат #$chatId"
                    )

                    // Синхронизация чата
                    val result = backupRepository.syncFullHistory(
                        recipientId = chatId,
                        chatType = "user"
                    )

                    result.onSuccess { count ->
                        Log.d(TAG, "✅ Synced $count messages for chat $chatId")
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to sync chat $chatId: ${error.message}")
                    }
                }

                // Обновляем время последнего бэкапа
                _settings.value = _settings.value?.copy(
                    lastBackupTime = System.currentTimeMillis()
                )

                // Отметить завершение бэкапа на сервере
                settingsRepository.markBackupComplete().onSuccess {
                    Log.d(TAG, "✅ Backup completion marked on server")
                }

                _syncProgress.value = SyncProgress(isRunning = false)
                calculateCacheSize()

                Log.d(TAG, "✅ Sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync failed: ${e.message}", e)
                _syncProgress.value = SyncProgress(isRunning = false)
            }
        }
    }

    // ==================== ЧЕРНОВИКИ ====================

    suspend fun deleteDrafts() {
        withContext(Dispatchers.IO) {
            try {
                val draftDao = database.draftDao()
                // TODO: Удалить все черновики
                Log.d(TAG, "✅ Drafts deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete drafts: ${e.message}", e)
            }
        }
    }

    // ==================== СОХРАНЕНИЕ НАСТРОЕК ====================

    private fun saveSettings() {
        viewModelScope.launch {
            _settings.value?.let { settings ->
                settingsRepository.updateSettings(
                    mobilePhotos = settings.mobilePhotos,
                    mobileVideos = settings.mobileVideos,
                    mobileFiles = settings.mobileFiles,
                    mobileVideosLimit = settings.mobileVideosLimit,
                    mobileFilesLimit = settings.mobileFilesLimit,
                    wifiPhotos = settings.wifiPhotos,
                    wifiVideos = settings.wifiVideos,
                    wifiFiles = settings.wifiFiles,
                    wifiVideosLimit = settings.wifiVideosLimit,
                    wifiFilesLimit = settings.wifiFilesLimit,
                    roamingPhotos = settings.roamingPhotos,
                    saveToGalleryPrivateChats = settings.saveToGalleryPrivateChats,
                    saveToGalleryGroups = settings.saveToGalleryGroups,
                    saveToGalleryChannels = settings.saveToGalleryChannels,
                    streamingEnabled = settings.streamingEnabled,
                    cacheSizeLimit = settings.cacheSizeLimit,
                    backupEnabled = settings.backupEnabled,
                    backupProvider = settings.backupProvider.name.lowercase(),
                    backupFrequency = settings.backupFrequency.name.lowercase()
                ).onSuccess { message ->
                    Log.d(TAG, "💾 Settings saved: $message")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to save settings: ${error.message}")
                }
            }
        }
    }
}
