package com.worldmates.messenger.ui.groups

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.worldmates.messenger.R
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.utils.DecryptionUtility
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GroupsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GroupsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen() {
    val viewModel: GroupsViewModel = viewModel()
    val groupsState by viewModel.groupList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val isCreatingGroup by viewModel.isCreatingGroup.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }

    // Load available users when dialog should open
    LaunchedEffect(showCreateDialog) {
        if (showCreateDialog) {
            viewModel.loadAvailableUsers()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Группы", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { /* Поиск групп */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = { /* Настройки */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Еще")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать группу")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            if (isLoading) {
                // Индикатор загрузки
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка групп...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (groupsState.isEmpty()) {
                // Экран пустого состояния
                EmptyGroupsScreen(
                    onCreateGroupClick = { showCreateDialog = true }
                )
            } else {
                // Список групп
                GroupsList(groups = groupsState)
            }
        }
    }

    // Show Create Group Dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            availableUsers = availableUsers,
            onCreateGroup = { name, description, memberIds, isPrivate ->
                viewModel.createGroup(
                    name = name,
                    description = description,
                    memberIds = memberIds,
                    isPrivate = isPrivate,
                    onSuccess = { showCreateDialog = false }
                )
            },
            isLoading = isCreatingGroup
        )
    }
}

@Composable
fun GroupsList(groups: List<Group>) {
    // Разделяем группы на закрепленные и обычные
    val pinnedGroups = groups.filter { it.isPinned == true }
    val regularGroups = groups.filter { it.isPinned != true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Секция закрепленных групп
        if (pinnedGroups.isNotEmpty()) {
            item {
                GroupsSectionHeader(title = "Закрепленные", icon = Icons.Default.PushPin)
            }
            items(pinnedGroups) { group ->
                GroupItem(group = group, isPinned = true)
            }

            // Разделитель между секциями
            item {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }

        // Секция всех групп
        if (regularGroups.isNotEmpty()) {
            item {
                GroupsSectionHeader(title = "Все группы", icon = Icons.Default.People)
            }
            items(regularGroups) { group ->
                GroupItem(group = group, isPinned = false)
            }
        }
    }
}

@Composable
fun GroupsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupItem(group: Group, isPinned: Boolean) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .animateContentSize()
            .clickable(
                onClick = {
                    // Переход к деталям группы
                    val intent = Intent(context, GroupDetailsActivity::class.java).apply {
                        putExtra("GROUP_ID", group.id)
                        putExtra("GROUP_NAME", group.name)
                    }
                    context.startActivity(intent)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else if (isPinned) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар группы
            GroupAvatar(group = group)

            Spacer(modifier = Modifier.width(12.dp))

            // Основная информация
            GroupInfo(group = group, isPinned = isPinned)

            Spacer(modifier = Modifier.width(8.dp))

            // Правая колонка (время и счетчик)
            GroupRightColumn(group = group)
        }
    }
}

@Composable
fun GroupAvatar(group: Group) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Градиентный фон для аватара
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            getAvatarColor(group.id),
                            getAvatarColor(group.id + 1)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Первая буква названия группы
            Text(
                text = group.name.firstOrNull()?.uppercase() ?: "G",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        // Бейдж количества участников
        if (group.membersCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = group.membersCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Иконка приватности
        if (group.isPrivate) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Приватная группа",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun GroupInfo(group: Group, isPinned: Boolean) {
    Column(
        modifier = Modifier.weight(1f)
    ) {
        // Заголовок с названием и иконкой закрепления
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Закреплено",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = group.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Иконка статуса (админ/модератор)
                if (group.isAdmin) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Администратор",
                        modifier = Modifier
                            .size(14.dp)
                            .padding(start = 4.dp),
                        tint = Color(0xFFFFC107)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Описание группы
        if (!group.description.isNullOrEmpty()) {
            Text(
                text = group.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Информация об участниках
        Text(
            text = "${group.membersCount} участников • ${group.adminName}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GroupRightColumn(group: Group) {
    Column(
        horizontalAlignment = Alignment.End
    ) {
        // Время последнего обновления
        Text(
            text = formatGroupTime(group.updatedTime ?: group.createdTime),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Счетчик непрочитанных сообщений
        // Note: В текущей модели Group нет поля unreadCount, можно добавить позже
        // if (group.unreadCount > 0) {
        //     Badge(
        //         containerColor = MaterialTheme.colorScheme.primary,
        //         contentColor = MaterialTheme.colorScheme.onPrimary
        //     ) {
        //         Text(
        //             text = if (group.unreadCount > 99) "99+"
        //                    else group.unreadCount.toString(),
        //             fontSize = 10.sp,
        //             fontWeight = FontWeight.Bold
        //         )
        //     }
        // }

        // Иконка уведомлений
        // Note: В текущей модели Group нет поля isMuted, добавим позже
        // Icon(
        //     imageVector = Icons.Default.Notifications,
        //     contentDescription = "Уведомления",
        //     modifier = Modifier.size(18.dp),
        //     tint = MaterialTheme.colorScheme.primary
        // )
    }
}

@Composable
fun EmptyGroupsScreen(
    onCreateGroupClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = "Нет групп",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "У вас пока нет групп",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Создайте первую группу для общения с друзьями, коллегами или семьей",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateGroupClick,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать группу")
        }
    }
}

// Вспомогательные функции
fun getAvatarColor(groupId: Long): Color {
    val colors = listOf(
        Color(0xFFE57373), // Красный
        Color(0xFFBA68C8), // Фиолетовый
        Color(0xFF4FC3F7), // Голубой
        Color(0xFF4DB6AC), // Бирюзовый
        Color(0xFFAED581), // Зеленый
        Color(0xFFFFD54F), // Желтый
        Color(0xFFFF8A65), // Оранжевый
        Color(0xFF7986CB)  // Индиго
    )
    return colors[(groupId % colors.size).toInt()]
}

fun formatGroupTime(timestamp: Long): String {
    val date = Date(timestamp * 1000) // Конвертируем секунды в миллисекунды
    val calendar = Calendar.getInstance()
    calendar.time = date

    val now = Calendar.getInstance()

    return when {
        // Сегодня
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        // Вчера
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> {
            "Вчера"
        }
        // В этом году
        calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
        }
        // Ранее
        else -> {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
        }
    }
}

// Функция для расшифровки последнего сообщения
fun getDecryptedLastMessage(group: Group): String {
    // Если в модели Group будет добавлено поле lastMessage,
    // можно использовать DecryptionUtility для расшифровки
    return group.description ?: "Группа создана"
}

// Функция для определения иконки типа группы
fun getGroupTypeIcon(group: Group): ImageVector {
    return when {
        group.isPrivate -> Icons.Default.NotificationsOff
        group.membersCount > 100 -> Icons.Default.People
        else -> Icons.Default.People
    }
}