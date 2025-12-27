package com.worldmates.messenger.ui.channels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.Channel

// ==================== TELEGRAM-STYLE CHANNEL ITEM ====================

/**
 * Класичний мінімалістичний стиль для каналів
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар (маленький, круглий)
        if (channel.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = channel.avatarUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF667eea)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Інформація про канал
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (channel.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Опис або статистика
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatCount(channel.subscribersCount)} підписників",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.postsCount > 0) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "${formatCount(channel.postsCount)} постів",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Індикатор статусу
        if (channel.isAdmin) {
            Text(
                text = "Адмін",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2196F3)
            )
        } else if (channel.isPrivate) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Private",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 76.dp)
    )
}

// ==================== CHANNEL CARD ====================

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            ChannelAvatar(
                avatarUrl = channel.avatarUrl,
                channelName = channel.name,
                size = 56.dp,
                isVerified = channel.isVerified
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Channel Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (channel.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.description!!,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Subscribers count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PeopleAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(channel.subscribersCount),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Posts count
                    if (channel.postsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Article,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatCount(channel.postsCount),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Private indicator
                    if (channel.isPrivate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Private",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Subscribe button or admin badge
            if (channel.isAdmin) {
                AdminBadge()
            } else {
                SubscribeButtonCompact(
                    isSubscribed = channel.isSubscribed,
                    onToggle = { /* Handled in parent */ }
                )
            }
        }
    }
}

// ==================== CHANNEL AVATAR ====================

@Composable
fun ChannelAvatar(
    avatarUrl: String,
    channelName: String,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    isVerified: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = channelName,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder with gradient
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channelName.take(2).uppercase(),
                    fontSize = (size.value / 2.5).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Verified badge
        if (isVerified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==================== SUBSCRIBE BUTTON ====================

@Composable
fun SubscribeButton(
    isSubscribed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSubscribed) Color(0xFFE0E0E0) else Color(0xFF667eea),
            contentColor = if (isSubscribed) Color(0xFF666666) else Color.White
        )
    ) {
        Icon(
            if (isSubscribed) Icons.Default.Notifications else Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isSubscribed) "Підписаний" else "Підписатись",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SubscribeButtonCompact(
    isSubscribed: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (isSubscribed) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSubscribed) Color(0xFF4CAF50) else Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== ADMIN BADGE ====================

@Composable
fun AdminBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = "Admin",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Адмін",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935)
            )
        }
    }
}

// ==================== CHANNEL HEADER ====================

@Composable
fun ChannelHeader(
    channel: Channel,
    onBackClick: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Top bar with back and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2C3E50)
                )
            }

            if (onSettingsClick != null && channel.isAdmin) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF2C3E50)
                    )
                }
            }
        }

        // Channel info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChannelAvatar(
                avatarUrl = channel.avatarUrl,
                channelName = channel.name,
                size = 80.dp,
                isVerified = channel.isVerified
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                if (channel.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (channel.username != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${channel.username}",
                    fontSize = 14.sp,
                    color = Color(0xFF667eea),
                    fontWeight = FontWeight.Medium
                )
            }

            if (channel.description != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = channel.description!!,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChannelStat(
                    icon = Icons.Default.PeopleAlt,
                    value = formatCount(channel.subscribersCount),
                    label = "Підписників"
                )
                ChannelStat(
                    icon = Icons.Default.Article,
                    value = formatCount(channel.postsCount),
                    label = "Постів"
                )
            }
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
    }
}

@Composable
fun ChannelStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF667eea),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// ==================== CHANNEL INFO CARD ====================

@Composable
fun ChannelInfoCard(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Про канал",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (channel.description != null) {
                InfoRow(
                    icon = Icons.Default.Info,
                    label = "Опис",
                    value = channel.description!!
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (channel.category != null) {
                InfoRow(
                    icon = Icons.Default.Category,
                    label = "Категорія",
                    value = channel.category!!
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            InfoRow(
                icon = if (channel.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                label = "Тип",
                value = if (channel.isPrivate) "Приватний" else "Публічний"
            )

            // Посилання можна додати пізніше
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF2C3E50)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF667eea),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== UTILITY FUNCTIONS ====================

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
