package com.worldmates.messenger.ui.channels

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Channel
import com.worldmates.messenger.data.model.ChannelPost
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp

/**
 * Активність для перегляду деталей каналу та його постів
 */
class ChannelDetailsActivity : AppCompatActivity() {

    private lateinit var channelsViewModel: ChannelsViewModel
    private lateinit var detailsViewModel: ChannelDetailsViewModel
    private var channelId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Отримуємо channelId з Intent
        channelId = intent.getLongExtra("channel_id", 0)
        if (channelId == 0L) {
            Toast.makeText(this, "Помилка: канал не знайдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ініціалізуємо ThemeManager
        ThemeManager.initialize(this)

        // Ініціалізуємо ViewModels
        channelsViewModel = ViewModelProvider(this).get(ChannelsViewModel::class.java)
        detailsViewModel = ViewModelProvider(this).get(ChannelDetailsViewModel::class.java)

        setContent {
            WorldMatesThemedApp {
                ChannelDetailsScreen(
                    channelId = channelId,
                    channelsViewModel = channelsViewModel,
                    detailsViewModel = detailsViewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Завантажуємо дані каналу та пости
        channelsViewModel.refreshChannel(channelId)
        detailsViewModel.loadChannelDetails(channelId)
        detailsViewModel.loadChannelPosts(channelId)
    }
}

/**
 * Екран деталей каналу з постами
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelDetailsScreen(
    channelId: Long,
    channelsViewModel: ChannelsViewModel,
    detailsViewModel: ChannelDetailsViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    // States from ViewModels
    val subscribedChannels by channelsViewModel.subscribedChannels.collectAsState()
    val allChannels by channelsViewModel.channelList.collectAsState()
    val posts by detailsViewModel.posts.collectAsState()
    val isLoadingPosts by detailsViewModel.isLoading.collectAsState()
    val error by detailsViewModel.error.collectAsState()

    // Знаходимо канал
    val channel = subscribedChannels.find { it.id == channelId }
        ?: allChannels.find { it.id == channelId }

    // UI States
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showSubscribersDialog by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    // Завантажуємо підписників
    val subscribers by detailsViewModel.subscribers.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            detailsViewModel.loadChannelPosts(channelId)
            channelsViewModel.refreshChannel(channelId)
            refreshing = false
        }
    )

    // Показуємо помилки через Toast
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            // FAB для створення поста (тільки для адмінів)
            if (channel?.isAdmin == true) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = Color(0xFF667eea),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Створити пост",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
        ) {
            if (channel == null) {
                // Канал не знайдено
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Канал не знайдено",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackPressed) {
                        Text("Повернутися")
                    }
                }
            } else {
                // Відображаємо канал
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    // Шапка каналу
                    item {
                        ChannelHeader(
                            channel = channel,
                            onBackClick = onBackPressed,
                            onSettingsClick = if (channel.isAdmin) {
                                { Toast.makeText(context, "Налаштування (в розробці)", Toast.LENGTH_SHORT).show() }
                            } else null,
                            onSubscribersClick = {
                                detailsViewModel.loadSubscribers(channelId)
                                showSubscribersDialog = true
                            }
                        )
                    }

                    // Кнопка підписки (якщо не адмін)
                    if (!channel.isAdmin) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(16.dp)
                            ) {
                                SubscribeButton(
                                    isSubscribed = channel.isSubscribed,
                                    onToggle = {
                                        if (channel.isSubscribed) {
                                            channelsViewModel.unsubscribeChannel(
                                                channelId = channelId,
                                                onSuccess = {
                                                    Toast.makeText(context, "Ви відписалися від каналу", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { error ->
                                                    Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            channelsViewModel.subscribeChannel(
                                                channelId = channelId,
                                                onSuccess = {
                                                    Toast.makeText(context, "Ви підписалися на канал!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { error ->
                                                    Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Заголовок секції постів
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Column {
                                Text(
                                    text = "Пости • ${posts.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Список постів
                    if (posts.isEmpty() && !isLoadingPosts) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Поки що немає постів",
                                        fontSize = 16.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (channel.isAdmin) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Створіть перший пост!",
                                            fontSize = 14.sp,
                                            color = Color.Gray.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = posts.sortedByDescending { it.createdTime },
                            key = { it.id }
                        ) { post ->
                            ChannelPostCard(
                                post = post,
                                onPostClick = {
                                    Toast.makeText(context, "Відкрити пост (в розробці)", Toast.LENGTH_SHORT).show()
                                },
                                onReactionClick = { emoji ->
                                    detailsViewModel.addPostReaction(
                                        postId = post.id,
                                        emoji = emoji,
                                        onSuccess = {
                                            Toast.makeText(context, "Реакцію додано!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onCommentsClick = {
                                    detailsViewModel.loadComments(post.id)
                                    Toast.makeText(context, "Коментарі (в розробці)", Toast.LENGTH_SHORT).show()
                                },
                                onShareClick = {
                                    Toast.makeText(context, "Поділитися (в розробці)", Toast.LENGTH_SHORT).show()
                                },
                                onMoreClick = {
                                    Toast.makeText(context, "Більше опцій (в розробці)", Toast.LENGTH_SHORT).show()
                                },
                                canEdit = channel.isAdmin,
                                modifier = Modifier
                                    .padding(horizontal = 0.dp, vertical = 0.dp)
                                    .animateItemPlacement()
                            )
                        }
                    }

                    // Індикатор завантаження
                    if (isLoadingPosts && posts.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF667eea)
                                )
                            }
                        }
                    }

                    // Нижній відступ
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // Pull-to-refresh індикатор
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // Діалог створення поста (для адмінів)
        if (showCreatePostDialog && channel?.isAdmin == true) {
            CreatePostDialog(
                channelId = channelId,
                onDismiss = { showCreatePostDialog = false },
                onCreate = { text, mediaUrl ->
                    // Створюємо медіа якщо є URL
                    val media = if (!mediaUrl.isNullOrBlank()) {
                        listOf(
                            com.worldmates.messenger.data.model.PostMedia(
                                url = mediaUrl,
                                type = "image", // За замовчуванням вважаємо зображенням
                                filename = null
                            )
                        )
                    } else null

                    detailsViewModel.createPost(
                        channelId = channelId,
                        text = text,
                        media = media,
                        onSuccess = {
                            Toast.makeText(context, "Пост створено!", Toast.LENGTH_SHORT).show()
                            detailsViewModel.loadChannelPosts(channelId)
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                    showCreatePostDialog = false
                }
            )
        }

        // Діалог підписників
        if (showSubscribersDialog) {
            SubscribersDialog(
                subscribers = subscribers,
                onDismiss = { showSubscribersDialog = false }
            )
        }
    }
}

/**
 * Діалог для створення нового поста (тільки для адмінів)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    channelId: Long,
    onDismiss: () -> Unit,
    onCreate: (text: String, mediaUrl: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Створити новий пост",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст поста") },
                    placeholder = { Text("Введіть текст поста...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 10
                )

                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("URL медіа (опціонально)") },
                    placeholder = { Text("https://example.com/image.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "💡 Підтримуються зображення, відео та GIF",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onCreate(
                            text.trim(),
                            mediaUrl.trim().takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text("Опублікувати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

/**
 * Діалог для відображення списку підписників
 */
@Composable
fun SubscribersDialog(
    subscribers: List<com.worldmates.messenger.data.model.ChannelSubscriber>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Підписники • ${subscribers.size}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (subscribers.isEmpty()) {
                Text(
                    text = "Немає підписників",
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(subscribers) { subscriber ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Аватар
                            AsyncImage(
                                model = subscriber.avatarUrl,
                                contentDescription = subscriber.username,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Інфо
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subscriber.username,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2C3E50)
                                )
                                if (subscriber.isMuted) {
                                    Text(
                                        text = "Вимкнено сповіщення",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        if (subscriber != subscribers.last()) {
                            Divider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = Color(0xFFEEEEEE)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}
}
