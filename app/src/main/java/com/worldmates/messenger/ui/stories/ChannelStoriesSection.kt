package com.worldmates.messenger.ui.stories

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.Channel
import com.worldmates.messenger.data.model.Story

/**
 * Секція для відображення комерційних stories каналів
 * Окремий UI для рекламних/бізнес stories
 */
@Composable
fun ChannelStoriesSection(
    channelStories: List<Pair<Channel, List<Story>>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (channelStories.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFECB3)
                    )
                )
            )
            .padding(vertical = 16.dp)
    ) {
        // Заголовок секції
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Channel stories",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Канали та реклама",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        // Список channel stories
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = channelStories,
                key = { (channel, _) -> channel.id }
            ) { (channel, stories) ->
                ChannelStoryCard(
                    channel = channel,
                    stories = stories,
                    onClick = {
                        context.startActivity(Intent(context, StoryViewerActivity::class.java).apply {
                            putExtra("user_id", channel.ownerId)
                            putExtra("is_channel_story", true)
                        })
                    }
                )
            }
        }
    }
}

/**
 * Карточка channel story (більш детальна, ніж звичайна story)
 */
@Composable
fun ChannelStoryCard(
    channel: Channel,
    stories: List<Story>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Превью останньої story
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                stories.firstOrNull()?.mediaItems?.firstOrNull()?.let { media ->
                    AsyncImage(
                        model = media.filename,
                        contentDescription = "Story preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Градієнт знизу
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Значок каналу
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Color(0xFFFFD700)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Channel",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }

            // Інфо каналу
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = channel.avatarUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFFFD700), CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF212121)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                modifier = Modifier.size(12.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stories.sumOf { it.viewsCount }}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Заголовок останньої story (якщо є)
                stories.firstOrNull()?.title?.let { title ->
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = Color(0xFF616161),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Компактна версія channel stories для відображення в іншіх місцях
 */
@Composable
fun CompactChannelStoriesRow(
    channelStories: List<Pair<Channel, List<Story>>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (channelStories.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Спонсорські Stories",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = channelStories,
                key = { (channel, _) -> channel.id }
            ) { (channel, stories) ->
                CompactChannelStoryItem(
                    channel = channel,
                    storiesCount = stories.size,
                    onClick = {
                        context.startActivity(Intent(context, StoryViewerActivity::class.java).apply {
                            putExtra("user_id", channel.ownerId)
                            putExtra("is_channel_story", true)
                        })
                    }
                )
            }
        }
    }
}

/**
 * Компактний елемент channel story
 */
@Composable
fun CompactChannelStoryItem(
    channel: Channel,
    storiesCount: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Золотистий бордер для каналів
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA000)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp)
            )

            // Аватар каналу
            AsyncImage(
                model = channel.avatarUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentScale = ContentScale.Crop
            )

            // Значок зірки
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp),
                shape = CircleShape,
                color = Color(0xFFFFD700),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Channel",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Назва каналу
        Text(
            text = channel.name,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
