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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
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
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showPostOptions by remember { mutableStateOf(false) }
    var showEditPostDialog by remember { mutableStateOf(false) }
    var showStatisticsDialog by remember { mutableStateOf(false) }
    var showAdminsDialog by remember { mutableStateOf(false) }
    var showEditChannelDialog by remember { mutableStateOf(false) }
    var showChannelMenuDialog by remember { mutableStateOf(false) }
    var showChannelSettingsDialog by remember { mutableStateOf(false) }
    var showPostDetailDialog by remember { mutableStateOf(false) }
    var selectedPostForOptions by remember { mutableStateOf<ChannelPost?>(null) }
    var selectedPostForDetail by remember { mutableStateOf<ChannelPost?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    // Завантажуємо підписників, коментарі, статистику, адмінів
    val subscribers by detailsViewModel.subscribers.collectAsState()
    val comments by detailsViewModel.comments.collectAsState()
    val selectedPost by detailsViewModel.selectedPost.collectAsState()
    val isLoadingComments by detailsViewModel.isLoadingComments.collectAsState()
    val statistics by detailsViewModel.statistics.collectAsState()
    val admins by detailsViewModel.admins.collectAsState()

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

    // Автоматичне оновлення контенту каналу кожні 15 секунд
    LaunchedEffect(channelId) {
        while (true) {
            kotlinx.coroutines.delay(15000) // 15 секунд
            // Тихе оновлення без показу індикатора завантаження
            detailsViewModel.loadChannelPosts(channelId)
        }
    }

    Scaffold(
        floatingActionButton = {
            // FAB для створення поста (тільки для адмінів)
            if (channel?.isAdmin == true) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                .background(MaterialTheme.colorScheme.background)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                                { showChannelMenuDialog = true }
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
                                    .background(MaterialTheme.colorScheme.surface)
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
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Text(
                                    text = "Пости • ${posts.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (channel.isAdmin) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Створіть перший пост!",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                                    selectedPostForDetail = post
                                    detailsViewModel.loadComments(post.id)
                                    detailsViewModel.registerPostView(
                                        postId = post.id,
                                        onSuccess = { /* Просмотр зареєстровано */ },
                                        onError = { /* Помилка реєстрації, але не показуємо користувачу */ }
                                    )
                                    showPostDetailDialog = true
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
                                    showCommentsSheet = true
                                },
                                onShareClick = {
                                    // Використовуємо Android Share Intent для поширення поста
                                    val shareText = buildString {
                                        append(post.text)
                                        append("\n\n")
                                        append("Від: ${post.authorName ?: post.authorUsername ?: "Користувач #${post.authorId}"}")
                                        append("\n")
                                        append("Канал: ${channel?.name ?: "WorldMates Channel"}")
                                    }

                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }

                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Поділитися постом")
                                    context.startActivity(shareIntent)
                                },
                                onMoreClick = {
                                    selectedPostForOptions = post
                                    showPostOptions = true
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
                                    color = MaterialTheme.colorScheme.primary
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

        // Bottom sheet коментарів
        if (showCommentsSheet && selectedPost != null) {
            CommentsBottomSheet(
                post = selectedPost,
                comments = comments,
                isLoading = isLoadingComments,
                currentUserId = UserSession.userId ?: 0L,
                isAdmin = channel?.isAdmin ?: false,
                onDismiss = { showCommentsSheet = false },
                onAddComment = { text ->
                    selectedPost?.let { post ->
                        detailsViewModel.addComment(
                            postId = post.id,
                            text = text,
                            onSuccess = {
                                Toast.makeText(context, "Коментар додано!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onDeleteComment = { commentId ->
                    selectedPost?.let { post ->
                        detailsViewModel.deleteComment(
                            commentId = commentId,
                            postId = post.id,
                            onSuccess = {
                                Toast.makeText(context, "Коментар видалено", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onCommentReaction = { commentId, emoji ->
                    detailsViewModel.addCommentReaction(
                        commentId = commentId,
                        emoji = emoji,
                        onSuccess = {
                            Toast.makeText(context, "Реакцію додано!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        // Діалог детального перегляду поста
        if (showPostDetailDialog && selectedPostForDetail != null) {
            PostDetailDialog(
                post = selectedPostForDetail!!,
                comments = comments,
                isLoadingComments = isLoadingComments,
                currentUserId = UserSession.userId ?: 0L,
                isAdmin = channel?.isAdmin ?: false,
                onDismiss = { showPostDetailDialog = false },
                onReactionClick = { emoji ->
                    selectedPostForDetail?.let { post ->
                        detailsViewModel.addPostReaction(
                            postId = post.id,
                            emoji = emoji,
                            onSuccess = {
                                Toast.makeText(context, "Реакцію додано!", Toast.LENGTH_SHORT).show()
                                detailsViewModel.loadChannelPosts(channelId)
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onAddComment = { text ->
                    selectedPostForDetail?.let { post ->
                        detailsViewModel.addComment(
                            postId = post.id,
                            text = text,
                            onSuccess = {
                                Toast.makeText(context, "Коментар додано!", Toast.LENGTH_SHORT).show()
                                detailsViewModel.loadComments(post.id)
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onDeleteComment = { commentId ->
                    selectedPostForDetail?.let { post ->
                        detailsViewModel.deleteComment(
                            commentId = commentId,
                            postId = post.id,
                            onSuccess = {
                                Toast.makeText(context, "Коментар видалено", Toast.LENGTH_SHORT).show()
                                detailsViewModel.loadComments(post.id)
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onCommentReaction = { commentId, emoji ->
                    detailsViewModel.addCommentReaction(
                        commentId = commentId,
                        emoji = emoji,
                        onSuccess = {
                            Toast.makeText(context, "Реакцію додано!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        // Bottom sheet опцій поста
        if (showPostOptions && selectedPostForOptions != null) {
            PostOptionsBottomSheet(
                post = selectedPostForOptions!!,
                onDismiss = { showPostOptions = false },
                onPinClick = {
                    selectedPostForOptions?.let { post ->
                        detailsViewModel.togglePinPost(
                            postId = post.id,
                            isPinned = post.isPinned,
                            onSuccess = {
                                val message = if (post.isPinned) "Пост відкріплено" else "Пост закріплено"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onEditClick = {
                    showEditPostDialog = true
                },
                onDeleteClick = {
                    selectedPostForOptions?.let { post ->
                        detailsViewModel.deletePost(
                            postId = post.id,
                            onSuccess = {
                                Toast.makeText(context, "Пост видалено", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        // Діалог редагування поста
        if (showEditPostDialog && selectedPostForOptions != null) {
            EditPostDialog(
                post = selectedPostForOptions!!,
                onDismiss = {
                    showEditPostDialog = false
                    selectedPostForOptions = null
                },
                onSave = { newText ->
                    selectedPostForOptions?.let { post ->
                        detailsViewModel.updatePost(
                            postId = post.id,
                            text = newText,
                            onSuccess = {
                                Toast.makeText(context, "Пост оновлено!", Toast.LENGTH_SHORT).show()
                                showEditPostDialog = false
                                selectedPostForOptions = null
                            },
                            onError = { error ->
                                Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        // Діалог статистики (тільки для адмінів)
        if (showStatisticsDialog && channel?.isAdmin == true) {
            StatisticsDialog(
                statistics = statistics,
                onDismiss = { showStatisticsDialog = false }
            )
        }

        // Діалог управління адмінами (тільки для власника)
        if (showAdminsDialog && channel?.isAdmin == true) {
            ManageAdminsDialog(
                admins = admins,
                onDismiss = { showAdminsDialog = false },
                onAddAdmin = { searchText, role ->
                    detailsViewModel.addChannelAdmin(
                        channelId = channelId,
                        userSearch = searchText,
                        role = role,
                        onSuccess = {
                            Toast.makeText(context, "Адміністратора додано!", Toast.LENGTH_SHORT).show()
                            detailsViewModel.loadChannelDetails(channelId)
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onRemoveAdmin = { userId ->
                    detailsViewModel.removeChannelAdmin(
                        channelId = channelId,
                        userId = userId,
                        onSuccess = {
                            Toast.makeText(context, "Адміністратора видалено", Toast.LENGTH_SHORT).show()
                            detailsViewModel.loadChannelDetails(channelId)
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        // Меню каналу (тільки для адмінів)
        if (showChannelMenuDialog && channel?.isAdmin == true) {
            AlertDialog(
                onDismissRequest = { showChannelMenuDialog = false },
                title = {
                    Text("Управління каналом", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Редагувати інформацію
                        TextButton(
                            onClick = {
                                showChannelMenuDialog = false
                                showEditChannelDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Редагувати інформацію")
                            }
                        }

                        // Налаштування
                        TextButton(
                            onClick = {
                                showChannelMenuDialog = false
                                showChannelSettingsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Налаштування")
                            }
                        }

                        // Статистика
                        TextButton(
                            onClick = {
                                showChannelMenuDialog = false
                                detailsViewModel.loadStatistics(channelId)
                                showStatisticsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Статистика")
                            }
                        }

                        // Адміністратори (тільки для адмінів)
                        if (channel.isAdmin) {
                            TextButton(
                                onClick = {
                                    showChannelMenuDialog = false
                                    detailsViewModel.loadChannelDetails(channelId)
                                    showAdminsDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Адміністратори")
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showChannelMenuDialog = false }) {
                        Text("Закрити")
                    }
                }
            )
        }

        // Діалог редагування інформації про канал
        if (showEditChannelDialog && channel?.isAdmin == true) {
            EditChannelInfoDialog(
                channel = channel,
                onDismiss = { showEditChannelDialog = false },
                onSave = { name, description, username ->
                    detailsViewModel.updateChannel(
                        channelId = channelId,
                        name = name,
                        description = description,
                        username = username,
                        onSuccess = { updatedChannel ->
                            Toast.makeText(context, "Канал оновлено!", Toast.LENGTH_SHORT).show()
                            showEditChannelDialog = false
                            detailsViewModel.loadChannelDetails(channelId)
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // Діалог налаштувань каналу
        if (showChannelSettingsDialog && channel?.isAdmin == true) {
            ChannelSettingsDialog(
                currentSettings = channel.settings,
                onDismiss = { showChannelSettingsDialog = false },
                onSave = { settings ->
                    detailsViewModel.updateChannelSettings(
                        channelId = channelId,
                        settings = settings,
                        onSuccess = {
                            Toast.makeText(context, "Налаштування збережено!", Toast.LENGTH_SHORT).show()
                            showChannelSettingsDialog = false
                            detailsViewModel.loadChannelDetails(channelId)
                        },
                        onError = { error ->
                            Toast.makeText(context, "Помилка: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                    containerColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                            if (!subscriber.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = subscriber.avatarUrl,
                                    contentDescription = subscriber.username,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (subscriber.username?.take(1) ?: "U").uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Інфо
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subscriber.name ?: subscriber.username ?: "User #${subscriber.id ?: subscriber.userId ?: "?"}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (subscriber.role != null) {
                                        Text(
                                            text = when(subscriber.role) {
                                                "owner" -> "Власник"
                                                "admin" -> "Адміністратор"
                                                else -> subscriber.role!!
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (subscriber.isMuted) {
                                        if (subscriber.role != null) {
                                            Text(
                                                text = "•",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                        Text(
                                            text = "Вимкнено сповіщення",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        if (subscriber != subscribers.last()) {
                            Divider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
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
