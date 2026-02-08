package com.worldmates.messenger.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.utils.EncryptedMediaHandler

/**
 * 🔍 MEDIA SEARCH SCREEN
 *
 * Экран поиска медиа-файлов в чатах
 * - Поддержка личных и групповых чатов
 * - Фильтры по типу медиа (фото, видео, аудио, файлы)
 * - Grid layout для фото/видео
 * - List layout для аудио/файлов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSearchScreen(
    chatId: Long? = null,
    groupId: Long? = null,
    onDismiss: () -> Unit,
    onMediaClick: (Message) -> Unit,
    viewModel: MediaSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(chatId, groupId) {
        viewModel.setChatId(chatId, groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Поиск медиа",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, "Очистить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 🔍 Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🎨 Filter Chips
            MediaFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.selectFilter(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 📊 Results
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isEmpty()) {
                                    "Введите запрос для поиска"
                                } else {
                                    "Ничего не найдено"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    MediaResultsGrid(
                        messages = searchResults,
                        filter = selectedFilter,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }
    }
}

/**
 * 🔍 Search Bar Component
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Поиск медиа...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Поиск")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "Очистить")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 🎨 Media Filter Chips
 */
@Composable
private fun MediaFilterChips(
    selectedFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

/**
 * 📊 Media Results Grid
 */
@Composable
private fun MediaResultsGrid(
    messages: List<Message>,
    filter: MediaFilter,
    onMediaClick: (Message) -> Unit
) {
    when (filter) {
        MediaFilter.ALL, MediaFilter.PHOTO, MediaFilter.VIDEO -> {
            // Grid layout для фото/видео
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    MediaGridItem(
                        message = message,
                        onClick = { onMediaClick(message) }
                    )
                }
            }
        }
        MediaFilter.AUDIO, MediaFilter.FILE -> {
            // List layout для аудио/файлов
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MediaListItem(
                        message = message,
                        onClick = { onMediaClick(message) }
                    )
                }
            }
        }
    }
}

/**
 * 📸 Media Grid Item (Photo/Video)
 */
@Composable
private fun MediaGridItem(
    message: Message,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val mediaUrl = message.decryptedMediaUrl ?: message.mediaUrl

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = EncryptedMediaHandler.getFullMediaUrl(mediaUrl, message.type),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Индикатор видео
        if (message.type == "video") {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Видео",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Длительность (если есть)
        if (message.mediaDuration != null && message.mediaDuration > 0) {
            Text(
                text = formatDuration(message.mediaDuration),
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * 📄 Media List Item (Audio/File)
 */
@Composable
private fun MediaListItem(
    message: Message,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка типа файла
            Icon(
                imageVector = when (message.type) {
                    "audio", "voice" -> Icons.Default.AudioFile
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Информация о файле
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.mediaUrl?.substringAfterLast("/") ?: "Файл",
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message.mediaSize != null && message.mediaSize > 0) {
                        Text(
                            text = formatFileSize(message.mediaSize),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (message.mediaDuration != null && message.mediaDuration > 0) {
                        Text(
                            text = formatDuration(message.mediaDuration),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Кнопка загрузки
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Скачать",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 🎨 Media Filter Enum
 */
enum class MediaFilter(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val mediaTypes: List<String>
) {
    ALL("Все", Icons.Default.GridView, listOf("image", "video", "audio", "voice", "file")),
    PHOTO("Фото", Icons.Default.Image, listOf("image")),
    VIDEO("Видео", Icons.Default.VideoLibrary, listOf("video")),
    AUDIO("Аудио", Icons.Default.AudioFile, listOf("audio", "voice")),
    FILE("Файлы", Icons.Default.InsertDriveFile, listOf("file"))
}

/**
 * 🕐 Format duration (seconds to MM:SS)
 */
private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

/**
 * 📦 Format file size
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
