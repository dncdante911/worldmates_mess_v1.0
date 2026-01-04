package com.worldmates.messenger.ui.stories

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Story
import com.worldmates.messenger.data.model.StoryComment
import com.worldmates.messenger.network.StoryReactionType
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity для перегляду Stories (як особистих, так і канальних)
 */
class StoryViewerActivity : AppCompatActivity() {

    private lateinit var viewModel: StoryViewModel
    private var storyId: Long = 0
    private var userId: Long = 0
    private var isChannelStory: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storyId = intent.getLongExtra("story_id", 0)
        userId = intent.getLongExtra("user_id", 0)
        isChannelStory = intent.getBooleanExtra("is_channel_story", false)

        viewModel = ViewModelProvider(this).get(StoryViewModel::class.java)

        // Завантажуємо story
        if (storyId > 0) {
            viewModel.loadStoryById(storyId)
        } else if (userId > 0) {
            viewModel.loadUserStories(userId)
        }

        setContent {
            WorldMatesThemedApp {
                StoryViewerScreen(
                    viewModel = viewModel,
                    isChannelStory = isChannelStory,
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    viewModel: StoryViewModel,
    isChannelStory: Boolean,
    onClose: () -> Unit
) {
    val currentStory by viewModel.currentStory.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val viewers by viewModel.viewers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showComments by remember { mutableStateOf(false) }
    var showViewers by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Прогрес перегляду story
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Автоматичний прогрес story
    LaunchedEffect(currentStory, isPaused) {
        if (currentStory != null && !isPaused) {
            val duration = if (currentStory!!.mediaItems.firstOrNull()?.type == "video") {
                currentStory!!.mediaItems.firstOrNull()?.duration?.times(1000L) ?: 15000L // 15 sec for videos without duration
            } else {
                20000L // 20 секунд для фото (было 5 секунд)
            }

            val steps = 100
            val stepDelay = duration / steps

            for (i in 0..steps) {
                if (!isPaused) {
                    progress = i.toFloat() / steps
                    delay(stepDelay)
                }
            }

            // Автоматично закриваємо після завершення
            if (progress >= 1f) {
                onClose()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Тап зліва - попередня story, справа - наступна
                        if (offset.x < size.width / 2) {
                            // TODO: Previous story
                        } else {
                            onClose() // TODO: Next story
                        }
                    },
                    onLongPress = {
                        isPaused = !isPaused
                    }
                )
            }
    ) {
        currentStory?.let { story ->
            android.util.Log.d("StoryViewer", "Displaying story: id=${story.id}, mediaItems count=${story.mediaItems.size}")

            // Медіа контент
            story.mediaItems.firstOrNull()?.let { media ->
                // Build full URL for media
                val mediaUrl = if (media.filename.startsWith("http")) {
                    media.filename // Already full URL
                } else {
                    "${Constants.MEDIA_BASE_URL}${media.filename}" // Add base URL
                }

                android.util.Log.d("StoryViewer", "Loading media: type=${media.type}, filename=${media.filename}")
                android.util.Log.d("StoryViewer", "Full URL: $mediaUrl")

                when (media.type) {
                    "image" -> {
                        // Show image with AsyncImage
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Story image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    "video" -> {
                        // Show video with ExoPlayer
                        VideoPlayer(
                            videoUrl = mediaUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        // Unknown media type
                        Text(
                            text = "Непідтримуваний тип медіа: ${media.type}",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } ?: run {
                android.util.Log.e("StoryViewer", "No media items found for story id=${story.id}")
            }

            // Прогрес бар вгорі
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            // Header з інфо про автора
            StoryHeader(
                story = story,
                isChannelStory = isChannelStory,
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Footer з діями
            StoryFooter(
                story = story,
                onReactionClick = { showReactions = true },
                onCommentClick = {
                    viewModel.loadComments(story.id)
                    showComments = true
                },
                onShareClick = { /* TODO: Share */ },
                onViewsClick = {
                    if (story.userId == UserSession.userId) {
                        viewModel.loadStoryViews(story.id)
                        showViewers = true
                    }
                },
                isOwnStory = story.userId == UserSession.userId,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        // Loading
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // Error
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }

    // Bottom sheets
    if (showComments) {
        CommentsBottomSheet(
            comments = comments,
            onDismiss = { showComments = false },
            onAddComment = { text ->
                currentStory?.let {
                    viewModel.createComment(it.id, text)
                }
            },
            onDeleteComment = { commentId ->
                viewModel.deleteComment(commentId)
            }
        )
    }

    if (showViewers) {
        ViewersBottomSheet(
            viewers = viewers,
            onDismiss = { showViewers = false }
        )
    }

    if (showReactions) {
        ReactionsBottomSheet(
            onDismiss = { showReactions = false },
            onReactionSelect = { reactionType ->
                currentStory?.let {
                    viewModel.reactToStory(it.id, reactionType)
                }
                showReactions = false
            }
        )
    }
}

@Composable
fun StoryHeader(
    story: Story,
    isChannelStory: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        AsyncImage(
            model = story.userData?.avatar,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Ім'я та час
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = story.userData?.name ?: "Unknown",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isChannelStory) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Channel",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = formatStoryTime(story.time),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        // Кнопка закриття
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
fun StoryFooter(
    story: Story,
    onReactionClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewsClick: () -> Unit,
    isOwnStory: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Опис story
        if (!story.title.isNullOrEmpty() || !story.description.isNullOrEmpty()) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                story.title?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                story.description?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Дії
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Реакції
            StoryActionButton(
                icon = Icons.Default.Favorite,
                text = "${story.reactions.total}",
                onClick = onReactionClick
            )

            // Коментарі
            StoryActionButton(
                icon = Icons.Default.Chat,
                text = "${story.commentsCount}",
                onClick = onCommentClick
            )

            // Поширити
            StoryActionButton(
                icon = Icons.Default.Share,
                text = "",
                onClick = onShareClick
            )

            // Перегляди (тільки для власних stories)
            if (isOwnStory) {
                StoryActionButton(
                    icon = Icons.Default.Visibility,
                    text = "${story.viewsCount}",
                    onClick = onViewsClick
                )
            }
        }
    }
}

@Composable
fun StoryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<StoryComment>,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Коментарі (${comments.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Список коментарів
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 400.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        onDelete = { onDeleteComment(comment.id) }
                    )
                }
            }

            // Поле введення коментаря
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Додати коментар...") },
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Gray
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: StoryComment,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.userData?.avatar,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userData?.name ?: "Unknown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.text,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = formatStoryTime(comment.time),
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Видалення (тільки для власних коментарів)
        if (comment.userId == UserSession.userId) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewersBottomSheet(
    viewers: List<com.worldmates.messenger.data.model.StoryViewer>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Переглянули (${viewers.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                items(viewers) { viewer ->
                    ViewerItem(viewer = viewer)
                }
            }
        }
    }
}

@Composable
fun ViewerItem(viewer: com.worldmates.messenger.data.model.StoryViewer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = viewer.avatar,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewer.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatStoryTime(viewer.time),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionsBottomSheet(
    onDismiss: () -> Unit,
    onReactionSelect: (StoryReactionType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Виберіть реакцію",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "👍" to StoryReactionType.LIKE,
                    "❤️" to StoryReactionType.LOVE,
                    "😂" to StoryReactionType.HAHA,
                    "😮" to StoryReactionType.WOW,
                    "😢" to StoryReactionType.SAD,
                    "😡" to StoryReactionType.ANGRY
                ).forEach { (emoji, type) ->
                    Text(
                        text = emoji,
                        fontSize = 40.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReactionSelect(type) }
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE // Loop video
        }
    }

    // Clean up player when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // ExoPlayer UI
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide default controls for Stories
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        modifier = modifier
    )
}

private fun formatStoryTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp

    return when {
        diff < 60 -> "щойно"
        diff < 3600 -> "${diff / 60} хв тому"
        diff < 86400 -> "${diff / 3600} год тому"
        else -> SimpleDateFormat("dd MMM", Locale("uk")).format(Date(timestamp * 1000))
    }
}
