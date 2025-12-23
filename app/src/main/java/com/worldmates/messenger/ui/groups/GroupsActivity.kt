// ============ GroupsActivity.kt ============

package com.worldmates.messenger.ui.groups

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.ui.messages.MessagesActivity
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp

class GroupsActivity : AppCompatActivity() {

    private lateinit var viewModel: GroupsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізуємо ThemeManager
        ThemeManager.initialize(this)

        viewModel = ViewModelProvider(this).get(GroupsViewModel::class.java)

        setContent {
            WorldMatesThemedApp {
                ModernGroupsScreen(
                    viewModel = viewModel,
                    onGroupClick = { group ->
                        navigateToGroupMessages(group)
                    },
                    onGroupDetails = { group ->
                        navigateToGroupDetails(group)
                    },
                    onBackPressed = { finish() }
                )
            }
        }
    }

    private fun navigateToGroupMessages(group: Group) {
        startActivity(Intent(this, MessagesActivity::class.java).apply {
            putExtra("group_id", group.id)
            putExtra("recipient_name", group.name)
            putExtra("recipient_avatar", group.avatarUrl)
            putExtra("is_group", true)
        })
    }

    private fun navigateToGroupDetails(group: Group) {
        startActivity(Intent(this, GroupDetailsActivity::class.java).apply {
            putExtra("group_id", group.id)
        })
    }
}

/**
 * Сучасний екран груп з пошуком, фільтрами та красивими картками
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernGroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (Group) -> Unit,
    onGroupDetails: (Group) -> Unit,
    onBackPressed: () -> Unit
) {
    val groups by viewModel.groupList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val isCreatingGroup by viewModel.isCreatingGroup.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(GroupFilter.ALL) }

    val context = LocalContext.current

    // Завантажуємо доступних користувачів
    LaunchedEffect(Unit) {
        viewModel.loadAvailableUsers()
    }

    // Фільтруємо групи за пошуком та фільтром
    val filteredGroups = remember(groups, searchQuery, selectedFilter) {
        groups.filter { group ->
            val matchesSearch = searchQuery.isEmpty() ||
                    group.name.contains(searchQuery, ignoreCase = true) ||
                    group.description?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = when (selectedFilter) {
                GroupFilter.ALL -> true
                GroupFilter.MY_GROUPS -> group.isOwner == true
                GroupFilter.PRIVATE -> group.isPrivate
                GroupFilter.PUBLIC -> !group.isPrivate
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Групи",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${groups.size} ${getGroupsCountText(groups.size)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка налаштувань
                    IconButton(onClick = { /* Налаштування груп */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Красива FAB з анімацією
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Створити групу") },
                containerColor = Color(0xFF667EEA),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Діалог створення групи
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
                                onSuccess = {
                                    showCreateDialog = false
                                }
                            )
                        },
                        isLoading = isCreatingGroup
                    )
                }

                // Діалог редагування групи
                groupToEdit?.let { group ->
                    EditGroupDialog(
                        group = group,
                        onDismiss = { groupToEdit = null },
                        onUpdate = { newName ->
                            viewModel.updateGroup(
                                groupId = group.id,
                                name = newName
                            )
                            groupToEdit = null
                        },
                        onDelete = {
                            viewModel.deleteGroup(group.id)
                            groupToEdit = null
                        },
                        onUploadAvatar = { uri ->
                            viewModel.uploadGroupAvatar(
                                groupId = group.id,
                                imageUri = uri,
                                context = context
                            )
                        },
                        isLoading = isLoading
                    )
                }

                // Пошукова панель
                GroupsSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )

                // Фільтри
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupFilter.values().forEach { filter ->
                        GroupFilterChip(
                            label = filter.label,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            icon = {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                // Контент
                when {
                    isLoading && groups.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF667EEA))
                        }
                    }
                    error != null -> {
                        ErrorState(
                            message = error!!,
                            onRetry = { viewModel.loadGroups() }
                        )
                    }
                    filteredGroups.isEmpty() && searchQuery.isNotEmpty() -> {
                        NoSearchResults(searchQuery = searchQuery)
                    }
                    filteredGroups.isEmpty() -> {
                        EmptyGroupsPlaceholder(
                            onCreateClick = { showCreateDialog = true }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = filteredGroups,
                                key = { it.id }
                            ) { group ->
                                ModernGroupCard(
                                    group = group,
                                    onClick = { onGroupClick(group) },
                                    onLongClick = { groupToEdit = group },
                                    isPinned = group.isPinned ?: false,
                                    unreadCount = 0, // TODO: отримати з viewModel
                                    modifier = Modifier.animateItemPlacement(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Стан помилки
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFEF5350)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Помилка",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667EEA)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Спробувати ще раз")
        }
    }
}

/**
 * Немає результатів пошуку
 */
@Composable
fun NoSearchResults(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Нічого не знайдено",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Не вдалося знайти групи за запитом \"$searchQuery\"",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Enum для фільтрів груп
 */
enum class GroupFilter(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("Всі", Icons.Default.Group),
    MY_GROUPS("Мої", Icons.Default.Person),
    PRIVATE("Приватні", Icons.Default.Lock),
    PUBLIC("Публічні", Icons.Default.Public)
}

/**
 * Хелпер для тексту кількості груп
 */
private fun getGroupsCountText(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "група"
        count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "групи"
        else -> "груп"
    }
}
