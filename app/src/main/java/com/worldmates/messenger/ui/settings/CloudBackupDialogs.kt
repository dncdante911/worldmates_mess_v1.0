package com.worldmates.messenger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.worldmates.messenger.data.model.CloudBackupSettings

/**
 * 📦 Диалог настройки автозагрузки медиа
 */
@Composable
fun MediaDownloadDialog(
    title: String,
    settings: CloudBackupSettings,
    isMobile: Boolean,
    onDismiss: () -> Unit,
    onSave: (photos: Boolean, videos: Boolean, files: Boolean, videoLimit: Int, fileLimit: Int) -> Unit
) {
    var photos by remember {
        mutableStateOf(if (isMobile) settings.mobilePhotos else settings.wifiPhotos)
    }
    var videos by remember {
        mutableStateOf(if (isMobile) settings.mobileVideos else settings.wifiVideos)
    }
    var files by remember {
        mutableStateOf(if (isMobile) settings.mobileFiles else settings.wifiFiles)
    }
    var videoLimit by remember {
        mutableStateOf(if (isMobile) settings.mobileVideosLimit else settings.wifiVideosLimit)
    }
    var fileLimit by remember {
        mutableStateOf(if (isMobile) settings.mobileFilesLimit else settings.wifiFilesLimit)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                item {
                    Text(
                        "Автоматически загружать медиафайлы",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Фото
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = photos,
                            onCheckedChange = { photos = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Фото", modifier = Modifier.weight(1f))
                    }
                }

                // Видео с лимитом
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = videos,
                            onCheckedChange = { videos = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Видео")
                            if (videos) {
                                Text(
                                    "До $videoLimit МБ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = videoLimit.toFloat(),
                                    onValueChange = { videoLimit = it.toInt() },
                                    valueRange = 1f..50f,
                                    steps = 49,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Файлы с лимитом
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = files,
                            onCheckedChange = { files = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Файлы")
                            if (files) {
                                Text(
                                    "До $fileLimit МБ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = fileLimit.toFloat(),
                                    onValueChange = { fileLimit = it.toInt() },
                                    valueRange = 1f..20f,
                                    steps = 19,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(photos, videos, files, videoLimit, fileLimit)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * 📦 Диалог выбора размера кэша
 */
@Composable
fun CacheSizeDialog(
    currentSize: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val sizes = remember {
        listOf(
            CloudBackupSettings.CACHE_SIZE_1GB to "1 GB",
            CloudBackupSettings.CACHE_SIZE_3GB to "3 GB",
            CloudBackupSettings.CACHE_SIZE_5GB to "5 GB",
            CloudBackupSettings.CACHE_SIZE_10GB to "10 GB",
            CloudBackupSettings.CACHE_SIZE_15GB to "15 GB",
            CloudBackupSettings.CACHE_SIZE_32GB to "32 GB",
            CloudBackupSettings.CACHE_SIZE_UNLIMITED to "Бесконечно"
        )
    }

    var selectedSize by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Максимальный размер кэша") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                sizes.forEach { (size, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedSize == size),
                                onClick = { selectedSize = size },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedSize == size),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(selectedSize)
            }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * 📦 Диалог выбора провайдера облачного хранилища
 */
@Composable
fun BackupProviderDialog(
    currentProvider: CloudBackupSettings.BackupProvider,
    onDismiss: () -> Unit,
    onSelect: (CloudBackupSettings.BackupProvider) -> Unit
) {
    val providers = remember {
        CloudBackupSettings.BackupProvider.values().toList()
    }

    var selectedProvider by remember { mutableStateOf(currentProvider) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите провайдер облака") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                providers.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedProvider == provider),
                                onClick = { selectedProvider = provider },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProvider == provider),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                provider.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (provider == CloudBackupSettings.BackupProvider.LOCAL_SERVER) {
                                Text(
                                    "Хранение на вашем сервере",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(selectedProvider)
            }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
