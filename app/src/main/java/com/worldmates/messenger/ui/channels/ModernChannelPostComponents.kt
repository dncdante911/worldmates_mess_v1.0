package com.worldmates.messenger.ui.channels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.*
import java.text.SimpleDateFormat
import java.util.*

// ==================== CHANNEL POST CARD ====================

@Composable
fun ChannelPostCard(
    post: ChannelPost,
    onPostClick: () -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    canEdit: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onPostClick),
        shape = RoundedCornerShape(20.dp), // Современные rounded corners
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Post Header - Современный стильный дизайн
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Аватар автора с красивой тенью и border
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        if (!post.authorAvatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = post.authorAvatar,
                                contentDescription = "Author Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Author",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.authorName ?: post.authorUsername ?: "Користувач #${post.authorId}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.15.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatPostTime(post.createdTime),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            if (post.isEdited) {
                                Text(
                                    text = "•",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "edited",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // More options - стильная кнопка
                if (canEdit) {
                    Surface(
                        onClick = onMoreClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Pinned indicator - стильный badge
            if (post.isPinned) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Закріплений пост",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Text - улучшенная типографика
            Text(
                text = post.text,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
                style = MaterialTheme.typography.bodyLarge
            )

            // Media Gallery
            if (!post.media.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostMediaGallery(media = post.media!!)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Stats - стильный badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCount(post.viewsCount),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "переглядів",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display existing reactions - стильные pill badges
            if (!post.reactions.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    post.reactions.forEach { reaction ->
                        Surface(
                            onClick = { onReactionClick(reaction.emoji) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (reaction.userReacted)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (reaction.userReacted) 2.dp else 1.dp,
                                color = if (reaction.userReacted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shadowElevation = if (reaction.userReacted) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reaction.emoji,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "${reaction.count}",
                                    fontSize = 14.sp,
                                    fontWeight = if (reaction.userReacted) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (reaction.userReacted)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Show avatars of first 2 users who reacted
                                if (!reaction.recentUsers.isNullOrEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                        reaction.recentUsers.take(2).forEach { user ->
                                            if (!user.avatar.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = user.avatar,
                                                    contentDescription = user.username,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.surface,
                                                            CircleShape
                                                        ),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Reactions - стильные кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("👍", "❤️", "🔥", "😂", "😮", "😢").forEach { emoji ->
                    Surface(
                        onClick = { onReactionClick(emoji) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // Action Buttons - стильные современные кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Comments button
                Surface(
                    onClick = onCommentsClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Comment,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        if (post.commentsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatCount(post.commentsCount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Коментарі",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Share button
                Surface(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Поділитись",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ==================== POST MEDIA GALLERY ====================

@Composable
fun PostMediaGallery(
    media: List<PostMedia>,
    modifier: Modifier = Modifier
) {
    when (media.size) {
        1 -> {
            // Single media
            PostMediaItem(
                media = media[0],
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        2 -> {
            // Two media side by side
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PostMediaItem(
                    media = media[0],
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                PostMediaItem(
                    media = media[1],
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
        else -> {
            // Grid for 3+ media
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PostMediaItem(
                        media = media[0],
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    PostMediaItem(
                        media = media[1],
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                if (media.size > 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PostMediaItem(
                            media = media[2],
                            modifier = Modifier
                                .weight(1f)
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (media.size > 3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                PostMediaItem(
                                    media = media[3],
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (media.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${media.size - 4}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostMediaItem(
    media: PostMedia,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (media.type) {
            "image" -> {
                AsyncImage(
                    model = media.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            "video" -> {
                AsyncImage(
                    model = media.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            else -> {
                // Placeholder for other media types
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// ==================== POST REACTIONS BAR ====================

@Composable
fun PostReactionsBar(
    reactions: List<PostReaction>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        reactions.forEach { reaction ->
            ReactionChip(
                emoji = reaction.emoji,
                count = reaction.count,
                isSelected = reaction.userReacted,
                onClick = { onReactionClick(reaction.emoji) }
            )
        }
    }
}

@Composable
fun ReactionChip(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp
            )
            Text(
                text = count.toString(),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== COMMENT CARD ====================

@Composable
fun CommentCard(
    comment: ChannelComment,
    onReactionClick: (String) -> Unit = {},
    onReplyClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    canDelete: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                // Author Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                        text = "U",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Author name and time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "User",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatPostTime(comment.time),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Comment text
                    Text(
                        text = comment.text,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Actions
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(
                            onClick = { onReactionClick("") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Реакція",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        TextButton(
                            onClick = onReplyClick,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Відповісти",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (canDelete) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SmallReactionChip(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = emoji, fontSize = 12.sp)
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ACTION BUTTON ====================

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ==================== UTILITY FUNCTIONS ====================

fun formatPostTime(timestamp: Long): String {
    // Конвертуємо timestamp з секунд в мілісекунди, якщо потрібно
    val timestampMs = if (timestamp < 10000000000L) timestamp * 1000 else timestamp

    val now = System.currentTimeMillis()
    val diff = now - timestampMs

    return when {
        diff < 60_000 -> "Щойно"
        diff < 3_600_000 -> "${diff / 60_000} хв"
        diff < 86_400_000 -> "${diff / 3_600_000} год"
        diff < 604_800_000 -> "${diff / 86_400_000} д"
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale("uk"))
            sdf.format(Date(timestampMs))
        }
    }
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

// ==================== COMMENTS COMPONENTS ====================

/**
 * Bottom sheet для відображення коментарів
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: ChannelPost?,
    comments: List<ChannelComment>,
    isLoading: Boolean,
    currentUserId: Long,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCommentReaction: (commentId: Long, emoji: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Коментарі",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${comments.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comments list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Коментарів ще немає",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            canDelete = isAdmin || comment.userId == currentUserId,
                            onDeleteClick = { onDeleteComment(comment.id) },
                            onReactionClick = { emoji -> onCommentReaction(comment.id, emoji) }
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add comment field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("Додати коментар...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
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
                        contentDescription = "Надіслати",
                        tint = if (commentText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Компонент для відображення одного коментаря з підтримкою тем
 */
@Composable
fun CommentItem(
    comment: ChannelComment,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
    onReactionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Avatar
        if (!comment.userAvatar.isNullOrEmpty()) {
            AsyncImage(
                model = comment.userAvatar,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // User name and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.userName ?: comment.username ?: "Користувач #${comment.userId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatPostTime(comment.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Comment text
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reaction buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                listOf("👍", "❤️", "😂").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onReactionClick(emoji) }
                            .padding(4.dp)
                    )
                }

                // Reactions count
                if (comment.reactionsCount > 0) {
                    Text(
                        text = "• ${comment.reactionsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        // Delete button
        if (canDelete) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Видалити",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== POST OPTIONS COMPONENTS ====================

/**
 * Bottom sheet з опціями для поста (Pin, Edit, Delete)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: ChannelPost,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Header
            Text(
                text = "Опції поста",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider()

            // Pin/Unpin option
            PostOptionItem(
                icon = if (post.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                text = if (post.isPinned) "Відкріпити" else "Закріпити",
                onClick = {
                    onPinClick()
                    onDismiss()
                }
            )

            // Edit option
            PostOptionItem(
                icon = Icons.Default.Edit,
                text = "Редагувати",
                onClick = {
                    onEditClick()
                    onDismiss()
                }
            )

            // Delete option
            PostOptionItem(
                icon = Icons.Default.Delete,
                text = "Видалити",
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDeleteClick()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Елемент опції поста
 */
@Composable
fun PostOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

/**
 * Діалог для редагування поста
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostDialog(
    post: ChannelPost,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editedText by remember { mutableStateOf(post.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редагувати пост",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 300.dp),
                    placeholder = { Text("Текст поста...") },
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editedText.isNotBlank()) {
                        onSave(editedText)
                    }
                },
                enabled = editedText.isNotBlank() && editedText != post.text
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

// ==================== STATISTICS DIALOG ====================

/**
 * Діалог статистики каналу
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDialog(
    statistics: ChannelStatistics?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статистика каналу", fontWeight = FontWeight.Bold) },
        text = {
            if (statistics == null) {
                CircularProgressIndicator()
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatItem("Підписників", "${statistics.subscribersCount}")
                    StatItem("Всього постів", "${statistics.postsCount}")
                    StatItem("Постів за тиждень", "${statistics.postsLastWeek}")
                    StatItem("Активних за 24 год", "${statistics.activeSubscribers24h}")

                    if (!statistics.topPosts.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Топ пости:", fontWeight = FontWeight.SemiBold)
                        statistics.topPosts.take(3).forEach { topPost ->
                            Text("• ${topPost.text.take(40)}... (${topPost.views} переглядів)", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрити") }
        }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// ==================== ADMIN MANAGEMENT ====================

/**
 * Діалог управління адмінами
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAdminsDialog(
    admins: List<ChannelAdmin>,
    onDismiss: () -> Unit,
    onAddAdmin: (String, String) -> Unit,
    onRemoveAdmin: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Адміністратори • ${admins.size}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                items(admins) { admin ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = admin.username.takeIf { !it.isNullOrEmpty() } ?: "Користувач #${admin.userId}",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                when(admin.role) {
                                    "owner" -> "Власник"
                                    "admin" -> "Адміністратор"
                                    else -> admin.role
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (admin.role != "owner") {
                            IconButton(onClick = { onRemoveAdmin(admin.userId) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAddDialog = true }) { Text("Додати") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрити") }
        }
    )

    if (showAddDialog) {
        AddAdminDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { searchText, role ->
                onAddAdmin(searchText, role)
                showAddDialog = false
            }
        )
    }
}

/**
 * Діалог додавання адміна
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdminDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("admin") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати адміністратора") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("ID, username або ім'я") },
                    placeholder = { Text("Введіть ID, @username або ім'я...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Роль:")
                    Spacer(Modifier.width(8.dp))
                    Row {
                        FilterChip(
                            selected = selectedRole == "admin",
                            onClick = { selectedRole = "admin" },
                            label = { Text("Адмін") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedRole == "moderator",
                            onClick = { selectedRole = "moderator" },
                            label = { Text("Модератор") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (searchText.isNotBlank()) {
                        onAdd(searchText.trim(), selectedRole)
                    }
                },
                enabled = searchText.isNotBlank()
            ) {
                Text("Додати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ==================== EDIT CHANNEL INFO DIALOG ====================

/**
 * Діалог редагування інформації про канал
 */
@Composable
fun EditChannelInfoDialog(
    channel: Channel,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, username: String) -> Unit
) {
    var channelName by remember { mutableStateOf(channel.name) }
    var channelDescription by remember { mutableStateOf(channel.description ?: "") }
    var channelUsername by remember { mutableStateOf(channel.username ?: "") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Редагувати канал",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Назва каналу
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Назва каналу") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null)
                    }
                )

                // Username
                OutlinedTextField(
                    value = channelUsername,
                    onValueChange = {
                        val cleaned = it.trim().lowercase()
                        channelUsername = cleaned
                        usernameError = when {
                            cleaned.isEmpty() -> null
                            !cleaned.matches(Regex("^[a-z0-9_]+$")) -> "Тільки літери, цифри та _"
                            cleaned.length < 5 -> "Мінімум 5 символів"
                            else -> null
                        }
                    },
                    label = { Text("Username (@username)") },
                    placeholder = { Text("channel_name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = usernameError != null,
                    supportingText = {
                        if (usernameError != null) {
                            Text(usernameError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null)
                    }
                )

                // Опис
                OutlinedTextField(
                    value = channelDescription,
                    onValueChange = { channelDescription = it },
                    label = { Text("Опис") },
                    placeholder = { Text("Розкажіть про ваш канал...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (channelName.isNotBlank() && usernameError == null) {
                        onSave(
                            channelName.trim(),
                            channelDescription.trim(),
                            channelUsername.trim()
                        )
                    }
                },
                enabled = channelName.isNotBlank() && usernameError == null
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

// ==================== CHANNEL SETTINGS DIALOG ====================

/**
 * Діалог налаштувань каналу
 */
@Composable
fun ChannelSettingsDialog(
    currentSettings: ChannelSettings?,
    onDismiss: () -> Unit,
    onSave: (ChannelSettings) -> Unit
) {
    // Якщо налаштування ще не завантажені, використовуємо дефолтні
    val defaultSettings = currentSettings ?: ChannelSettings()

    var allowComments by remember { mutableStateOf(defaultSettings.allowComments) }
    var allowReactions by remember { mutableStateOf(defaultSettings.allowReactions) }
    var allowShares by remember { mutableStateOf(defaultSettings.allowShares) }
    var showStatistics by remember { mutableStateOf(defaultSettings.showStatistics) }
    var notifySubscribers by remember { mutableStateOf(defaultSettings.notifySubscribersNewPost) }
    var signatureEnabled by remember { mutableStateOf(defaultSettings.signatureEnabled) }
    var commentsModeration by remember { mutableStateOf(defaultSettings.commentsModeration) }
    var slowModeSeconds by remember { mutableStateOf(defaultSettings.slowModeSeconds?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Налаштування каналу",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Публікації",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF667eea)
                )

                // Підпис автора
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Підпис автора поста", fontSize = 14.sp)
                        Text(
                            "Показувати ім'я автора в постах",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = signatureEnabled,
                        onCheckedChange = { signatureEnabled = it }
                    )
                }

                Divider()

                Text(
                    text = "Інтерактивність",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF667eea)
                )

                // Коментарі
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Дозволити коментарі", fontSize = 14.sp)
                        Text(
                            "Підписники можуть коментувати пости",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = allowComments,
                        onCheckedChange = { allowComments = it }
                    )
                }

                // Реакції
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Дозволити реакції", fontSize = 14.sp)
                        Text(
                            "Підписники можуть ставити реакції",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = allowReactions,
                        onCheckedChange = { allowReactions = it }
                    )
                }

                // Поширення
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Дозволити поширення", fontSize = 14.sp)
                        Text(
                            "Можна репостити пости каналу",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = allowShares,
                        onCheckedChange = { allowShares = it }
                    )
                }

                Divider()

                Text(
                    text = "Модерація",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF667eea)
                )

                // Модерація коментарів
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Модерація коментарів", fontSize = 14.sp)
                        Text(
                            "Коментарі потребують схвалення",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = commentsModeration,
                        onCheckedChange = { commentsModeration = it }
                    )
                }

                // Повільний режим
                OutlinedTextField(
                    value = slowModeSeconds,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            slowModeSeconds = it
                        }
                    },
                    label = { Text("Повільний режим (секунди)") },
                    placeholder = { Text("0 = вимкнено") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            "Затримка між коментарями (0-300 сек)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                )

                Divider()

                Text(
                    text = "Сповіщення та статистика",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF667eea)
                )

                // Сповіщення про нові пости
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Сповіщення про пости", fontSize = 14.sp)
                        Text(
                            "Надсилати push-сповіщення підписникам",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = notifySubscribers,
                        onCheckedChange = { notifySubscribers = it }
                    )
                }

                // Показувати статистику
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Показувати статистику", fontSize = 14.sp)
                        Text(
                            "Підписники бачать статистику каналу",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = showStatistics,
                        onCheckedChange = { showStatistics = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val slowMode = slowModeSeconds.toIntOrNull()
                    val validSlowMode = when {
                        slowMode == null || slowMode < 0 -> null
                        slowMode > 300 -> 300
                        else -> slowMode
                    }

                    val updatedSettings = ChannelSettings(
                        allowComments = allowComments,
                        allowReactions = allowReactions,
                        allowShares = allowShares,
                        showStatistics = showStatistics,
                        showViewsCount = defaultSettings.showViewsCount,
                        notifySubscribersNewPost = notifySubscribers,
                        autoDeletePostsDays = defaultSettings.autoDeletePostsDays,
                        signatureEnabled = signatureEnabled,
                        commentsModeration = commentsModeration,
                        allowForwarding = defaultSettings.allowForwarding,
                        slowModeSeconds = validSlowMode
                    )
                    onSave(updatedSettings)
                }
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}

/**
 * Діалог детального перегляду поста
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailDialog(
    post: ChannelPost,
    comments: List<ChannelComment>,
    isLoadingComments: Boolean,
    currentUserId: Long,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onReactionClick: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCommentReaction: (commentId: Long, emoji: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(
                "Деталі поста",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Автор поста
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!post.authorAvatar.isNullOrEmpty()) {
                        AsyncImage(
                            model = post.authorAvatar,
                            contentDescription = "Author Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF667eea)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF667eea)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Author",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.authorName ?: post.authorUsername ?: "Користувач #${post.authorId}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = formatPostTime(post.createdTime),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Текст поста
                Text(
                    text = post.text,
                    fontSize = 15.sp,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Медіа (якщо є)
                post.media?.forEach { media ->
                    when (media.type) {
                        "image" -> {
                            AsyncImage(
                                model = media.url,
                                contentDescription = "Post image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 8.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Статистика
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${post.viewsCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Переглядів",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${post.reactionsCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Реакцій",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${post.commentsCount}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Коментарів",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Реакції
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReactionEmojis.DEFAULT_REACTIONS.forEach { emoji ->
                        OutlinedButton(
                            onClick = { onReactionClick(emoji) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Заголовок коментарів
                Text(
                    text = "Коментарі (${comments.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Список коментарів
                if (isLoadingComments) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Коментарів ще немає",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        comments.take(5).forEach { comment ->
                            CommentItem(
                                comment = comment,
                                canDelete = isAdmin || comment.userId == currentUserId,
                                onDeleteClick = { onDeleteComment(comment.id) },
                                onReactionClick = { emoji -> onCommentReaction(comment.id, emoji) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (comments.size > 5) {
                            Text(
                                text = "і ще ${comments.size - 5} коментарів...",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Поле додавання коментаря
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Додати коментар...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

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
                            contentDescription = "Надіслати",
                            tint = if (commentText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

