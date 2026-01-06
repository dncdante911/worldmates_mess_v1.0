package com.worldmates.messenger.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldmates.messenger.data.model.CloudBackupSettings
import com.worldmates.messenger.data.model.SyncProgress
import kotlinx.coroutines.launch

/**
 * 📦 CLOUD BACKUP v2: Экран настроек данных и памяти
 *
 * Полный аналог Telegram с расширенными возможностями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: CloudBackupViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val scope = rememberCoroutineScope()

    var showMobileDataDialog by remember { mutableStateOf(false) }
    var showWiFiDialog by remember { mutableStateOf(false) }
    var showCacheSizeDialog by remember { mutableStateOf(false) }
    var showBackupProviderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Данные и память") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ==================== ИСПОЛЬЗОВАНИЕ СЕТИ И КЭША ====================
            item {
                SectionHeader("Использование сети и кэша")
            }

            // Использование памяти
            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Использование памяти",
                    subtitle = formatBytes(cacheSize),
                    onClick = { /* Navigate to storage details */ }
                )
            }

            // Использование трафика
            item {
                SettingsItem(
                    icon = Icons.Default.NetworkCheck,
                    title = "Использование трафика",
                    subtitle = "42.94 GB", // TODO: Real data
                    onClick = { /* Navigate to network usage */ }
                )
            }

            // ==================== АВТОЗАГРУЗКА МЕДИА ====================
            item {
                SectionHeader("Автозагрузка медиа")
            }

            // Через мобильную сеть
            item {
                settings?.let { s ->
                    SettingsItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Через мобильную сеть",
                        subtitle = buildMediaDownloadString(
                            s.mobilePhotos,
                            s.mobileVideos,
                            s.mobileFiles,
                            s.mobileVideosLimit,
                            s.mobileFilesLimit
                        ),
                        onClick = { showMobileDataDialog = true }
                    )
                }
            }

            // Через сети Wi-Fi
            item {
                settings?.let { s ->
                    SettingsItem(
                        icon = Icons.Default.Wifi,
                        title = "Через сети Wi-Fi",
                        subtitle = buildMediaDownloadString(
                            s.wifiPhotos,
                            s.wifiVideos,
                            s.wifiFiles,
                            s.wifiVideosLimit,
                            s.wifiFilesLimit
                        ),
                        onClick = { showWiFiDialog = true }
                    )
                }
            }

            // В роуминге
            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.FlightTakeoff,
                        title = "В роуминге",
                        subtitle = "Фото",
                        checked = s.roamingPhotos,
                        onCheckedChange = { viewModel.updateRoamingPhotos(it) }
                    )
                }
            }

            // Кнопка сброса настроек
            item {
                TextButton(
                    onClick = { viewModel.resetMediaSettings() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Сбросить настройки",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ==================== СОХРАНЯТЬ В ГАЛЕРЕЕ ====================
            item {
                SectionHeader("Сохранять в галерее")
            }

            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Chat,
                        title = "Личные чаты",
                        subtitle = if (s.saveToGalleryPrivateChats) "Фотографии, Видео" else "Нет",
                        checked = s.saveToGalleryPrivateChats,
                        onCheckedChange = { viewModel.updateSaveToGalleryPrivateChats(it) }
                    )
                }
            }

            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Group,
                        title = "Группы",
                        subtitle = "Нет",
                        checked = s.saveToGalleryGroups,
                        onCheckedChange = { viewModel.updateSaveToGalleryGroups(it) }
                    )
                }
            }

            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Campaign,
                        title = "Каналы",
                        subtitle = "Нет",
                        checked = s.saveToGalleryChannels,
                        onCheckedChange = { viewModel.updateSaveToGalleryChannels(it) }
                    )
                }
            }

            // ==================== СТРИМИНГ ====================
            item {
                SectionHeader("Стриминг")
            }

            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Stream,
                        title = "Стриминг аудиофайлов и видео",
                        subtitle = "Когда это возможно, приложение будет воспроизводить видеозаписи и музыку, не дожидаясь завершения загрузки.",
                        checked = s.streamingEnabled,
                        onCheckedChange = { viewModel.updateStreaming(it) }
                    )
                }
            }

            // ==================== УПРАВЛЕНИЕ КЭШЕМ ====================
            item {
                SectionHeader("Управление кэшем")
            }

            item {
                settings?.let { s ->
                    SettingsItem(
                        icon = Icons.Default.DataUsage,
                        title = "Максимальный размер кэша",
                        subtitle = CloudBackupSettings.cacheSizeToString(s.cacheSizeLimit),
                        onClick = { showCacheSizeDialog = true }
                    )
                }
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Очистить кэш",
                    subtitle = "Освободить ${formatBytes(cacheSize)}",
                    onClick = {
                        scope.launch {
                            viewModel.clearCache()
                        }
                    }
                )
            }

            // ==================== CLOUD BACKUP ====================
            item {
                SectionHeader("Облачный бэкап")
            }

            item {
                settings?.let { s ->
                    SettingsItemWithSwitch(
                        icon = Icons.Default.CloudUpload,
                        title = "Включить бэкап",
                        subtitle = "Автоматическое сохранение сообщений в облако",
                        checked = s.backupEnabled,
                        onCheckedChange = { viewModel.updateBackupEnabled(it) }
                    )
                }
            }

            item {
                settings?.let { s ->
                    if (s.backupEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "Провайдер облака",
                            subtitle = s.backupProvider.displayName,
                            onClick = { showBackupProviderDialog = true }
                        )
                    }
                }
            }

            item {
                settings?.let { s ->
                    if (s.backupEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Sync,
                            title = "Синхронизировать сейчас",
                            subtitle = s.lastBackupTime?.let { "Последний бэкап: ${formatTime(it)}" } ?: "Еще не синхронизировано",
                            onClick = {
                                scope.launch {
                                    viewModel.startSync()
                                }
                            }
                        )
                    }
                }
            }

            // Прогресс-бар синхронизации
            item {
                if (syncProgress.isRunning) {
                    SyncProgressBar(syncProgress)
                }
            }

            // ==================== ПРОКСИ ====================
            item {
                SectionHeader("Прокси")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.VpnKey,
                    title = "Настройки прокси",
                    subtitle = "Не используется",
                    onClick = { /* Navigate to proxy settings */ }
                )
            }

            // Удалить черновики
            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Удалить черновики",
                    subtitle = null,
                    onClick = {
                        scope.launch {
                            viewModel.deleteDrafts()
                        }
                    }
                )
            }

            // Отступ снизу
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ==================== ДИАЛОГИ ====================

    if (showMobileDataDialog) {
        MediaDownloadDialog(
            title = "Через мобильную сеть",
            settings = settings!!,
            isMobile = true,
            onDismiss = { showMobileDataDialog = false },
            onSave = { photos, videos, files, videoLimit, fileLimit ->
                viewModel.updateMobileDataSettings(photos, videos, files, videoLimit, fileLimit)
                showMobileDataDialog = false
            }
        )
    }

    if (showWiFiDialog) {
        MediaDownloadDialog(
            title = "Через сети Wi-Fi",
            settings = settings!!,
            isMobile = false,
            onDismiss = { showWiFiDialog = false },
            onSave = { photos, videos, files, videoLimit, fileLimit ->
                viewModel.updateWiFiSettings(photos, videos, files, videoLimit, fileLimit)
                showWiFiDialog = false
            }
        )
    }

    if (showCacheSizeDialog) {
        CacheSizeDialog(
            currentSize = settings!!.cacheSizeLimit,
            onDismiss = { showCacheSizeDialog = false },
            onSelect = { size ->
                viewModel.updateCacheSize(size)
                showCacheSizeDialog = false
            }
        )
    }

    if (showBackupProviderDialog) {
        BackupProviderDialog(
            currentProvider = settings!!.backupProvider,
            onDismiss = { showBackupProviderDialog = false },
            onSelect = { provider ->
                viewModel.updateBackupProvider(provider)
                showBackupProviderDialog = false
            }
        )
    }
}

// ==================== UI КОМПОНЕНТЫ ====================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SyncProgressBar(progress: SyncProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Синхронизация...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    progress.currentChatName?.let { chatName ->
                        Text(
                            text = chatName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${progress.progressPercent.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress.progressPercent / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${progress.currentItem} из ${progress.totalItems} чатов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

private fun buildMediaDownloadString(
    photos: Boolean,
    videos: Boolean,
    files: Boolean,
    videoLimit: Int,
    fileLimit: Int
): String {
    val parts = mutableListOf<String>()
    if (photos) parts.add("Фото")
    if (videos) parts.add("Видео ($videoLimit МБ)")
    if (files) parts.add("Файлы ($fileLimit МБ)")
    return if (parts.isEmpty()) "Нет" else parts.joinToString(", ")
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes.toFloat() / (1024 * 1024 * 1024)
    return "%.2f GB".format(gb)
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hours = diff / (1000 * 60 * 60)
    return when {
        hours < 1 -> "только что"
        hours < 24 -> "$hours ч. назад"
        else -> "${hours / 24} дн. назад"
    }
}
