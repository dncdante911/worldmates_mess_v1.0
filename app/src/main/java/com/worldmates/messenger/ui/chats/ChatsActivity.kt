package com.worldmates.messenger.ui.chats

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.worldmates.messenger.data.model.Chat
import com.worldmates.messenger.data.ContactNicknameRepository
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.worldmates.messenger.ui.messages.MessagesActivity
import com.worldmates.messenger.ui.theme.AnimatedGradientBackground
import com.worldmates.messenger.ui.theme.ChatGlassCard
import com.worldmates.messenger.ui.theme.ExpressiveFAB
import com.worldmates.messenger.ui.theme.ExpressiveIconButton
import com.worldmates.messenger.ui.theme.GlassTopAppBar
import com.worldmates.messenger.ui.theme.PulsingBadge
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WMColors
import com.worldmates.messenger.ui.theme.WMGradients
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp

class ChatsActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatsViewModel
    private lateinit var groupsViewModel: com.worldmates.messenger.ui.groups.GroupsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем ThemeManager
        ThemeManager.initialize(this)

        viewModel = ViewModelProvider(this).get(ChatsViewModel::class.java)
        groupsViewModel = ViewModelProvider(this).get(com.worldmates.messenger.ui.groups.GroupsViewModel::class.java)

        setContent {
            WorldMatesThemedApp {
                // Обробка необхідності перелогіну
                val needsRelogin by viewModel.needsRelogin.collectAsState()

                LaunchedEffect(needsRelogin) {
                    if (needsRelogin) {
                        // Перенаправляємо на екран логіну
                        navigateToLogin()
                        finish()
                    }
                }

                ChatsScreen(
                    viewModel = viewModel,
                    groupsViewModel = groupsViewModel,
                    onChatClick = { chat ->
                        navigateToMessages(chat)
                    },
                    onGroupClick = { group ->
                        navigateToGroupMessages(group)
                    },
                    onSettingsClick = {
                        navigateToSettings()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Оновлюємо список чатів при поверненні на екран
        viewModel.fetchChats()
        groupsViewModel.fetchGroups()
    }

    private fun navigateToMessages(chat: Chat) {
        startActivity(Intent(this, MessagesActivity::class.java).apply {
            putExtra("recipient_id", chat.userId)
            putExtra("recipient_name", chat.username)
            putExtra("recipient_avatar", chat.avatarUrl)
        })
    }

    private fun navigateToGroupMessages(group: com.worldmates.messenger.data.model.Group) {
        startActivity(Intent(this, MessagesActivity::class.java).apply {
            putExtra("group_id", group.id)
            putExtra("recipient_name", group.name)
            putExtra("recipient_avatar", group.avatarUrl)
            putExtra("is_group", true)
        })
    }

    private fun navigateToSettings() {
        startActivity(Intent(this, com.worldmates.messenger.ui.settings.SettingsActivity::class.java))
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, com.worldmates.messenger.ui.login.LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel,
    groupsViewModel: com.worldmates.messenger.ui.groups.GroupsViewModel,
    onChatClick: (Chat) -> Unit,
    onGroupClick: (com.worldmates.messenger.data.model.Group) -> Unit,
    onSettingsClick: () -> Unit
) {
    val chats by viewModel.chatList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val groups by groupsViewModel.groupList.collectAsState()
    val isLoadingGroups by groupsViewModel.isLoading.collectAsState()
    val errorGroups by groupsViewModel.error.collectAsState()
    val availableUsers by groupsViewModel.availableUsers.collectAsState()
    val isCreatingGroup by groupsViewModel.isCreatingGroup.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var showGroups by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var chatToRename by remember { mutableStateOf<Chat?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val nicknameRepository = remember { ContactNicknameRepository(context) }

    // Load available users when switching to groups tab
    LaunchedEffect(showGroups) {
        if (showGroups) {
            groupsViewModel.loadAvailableUsers()
        }
    }

    // Показуємо помилки через Snackbar
    LaunchedEffect(errorGroups) {
        errorGroups?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Фільтруємо тільки особисті чати (НЕ групи)
    val filteredChats = chats.filter {
        !it.isGroup && it.username?.contains(searchText, ignoreCase = true) == true
    }
    // Фільтруємо групи
    val filteredGroups = groups.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    // Telegram-style - простой цвет фона без градиента
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,  // Цвет фона из темы
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showGroups) {
                ExpressiveFAB(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = WMGradients.buttonGradient
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Створити групу",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        // Glass Header with expressive motion
        GlassTopAppBar(
            title = {
                Text(
                    "Повідомлення",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                var showSearchDialog by remember { mutableStateOf(false) }

                // Refresh button with expressive animation
                ExpressiveIconButton(onClick = {
                    if (showGroups) {
                        groupsViewModel.fetchGroups()
                    } else {
                        viewModel.fetchChats()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                }

                ExpressiveIconButton(onClick = { showSearchDialog = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Пошук користувачів")
                }
                ExpressiveIconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                }

                if (showSearchDialog) {
                    UserSearchDialog(
                        onDismiss = { showSearchDialog = false },
                        onUserClick = { user ->
                            showSearchDialog = false
                            // TODO: Navigate to messages with this user
                        }
                    )
                }
            }
        )

        // Search
        SearchBar(
            searchText = searchText,
            onSearchChange = { searchText = it }
        )

        // Tabs: Chats / Groups
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showGroups = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showGroups) Color(0xFF0084FF) else Color.LightGray
                )
            ) {
                Text("Чати", color = Color.White)
            }
            Button(
                onClick = { showGroups = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showGroups) Color(0xFF0084FF) else Color.LightGray
                )
            ) {
                Text("Групи", color = Color.White)
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                // Loading indicator
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0084FF))
                    Text(
                        "Завантаження...",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.Gray
                    )
                }
            } else if (error != null) {
                // Error state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "⚠️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        error ?: "Помилка завантаження",
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = { viewModel.fetchChats() },
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0084FF)
                        )
                    ) {
                        Text("Спробувати ще раз")
                    }
                }
            } else if (showGroups) {
                // Groups List
                if (isLoadingGroups) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0084FF))
                        Text(
                            "Завантаження груп...",
                            modifier = Modifier.padding(top = 16.dp),
                            color = Color.Gray
                        )
                    }
                } else if (errorGroups != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "⚠️",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            errorGroups ?: "Помилка завантаження груп",
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Button(
                            onClick = { groupsViewModel.fetchGroups() },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0084FF)
                            )
                        ) {
                            Text("Спробувати ще раз")
                        }
                    }
                } else if (filteredGroups.isEmpty()) {
                    EmptyGroupsState(onCreateClick = { showCreateGroupDialog = true })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredGroups) { group ->
                            GroupItemRow(
                                group = group,
                                onClick = { onGroupClick(group) }
                            )
                        }
                    }
                }
            } else {
                // Chats List
                if (filteredChats.isEmpty()) {
                    EmptyChatsState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredChats) { chat ->
                            val nickname by nicknameRepository.getNickname(chat.userId).collectAsState(initial = null)
                            ChatItemRow(
                                chat = chat,
                                nickname = nickname,
                                onClick = { onChatClick(chat) },
                                onLongPress = {
                                    chatToRename = chat
                                    showRenameDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Create Group Dialog
        if (showCreateGroupDialog) {
            com.worldmates.messenger.ui.groups.CreateGroupDialog(
                onDismiss = { showCreateGroupDialog = false },
                availableUsers = availableUsers,
                onCreateGroup = { name, description, memberIds, isPrivate ->
                    groupsViewModel.createGroup(
                        name = name,
                        description = description,
                        memberIds = memberIds,
                        isPrivate = isPrivate,
                        onSuccess = {
                            showCreateGroupDialog = false
                        }
                    )
                },
                isLoading = isCreatingGroup
            )
        }

        // Rename Contact Dialog
        if (showRenameDialog && chatToRename != null) {
            RenameContactDialog(
                chat = chatToRename!!,
                currentNickname = null, // будемо отримувати з repository в діалозі
                onDismiss = {
                    showRenameDialog = false
                    chatToRename = null
                },
                onSave = { nickname ->
                    scope.launch {
                        nicknameRepository.setNickname(chatToRename!!.userId, nickname)
                        showRenameDialog = false
                        chatToRename = null
                    }
                },
                nicknameRepository = nicknameRepository
            )
        }
    }  // Конец lambda paddingValues для Scaffold
    }  // Конец Scaffold
}  // Конец функции ChatsScreen

@Composable
fun SearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)  // Цвет из темы
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            modifier = Modifier.padding(horizontal = 12.dp),
            tint = colorScheme.onSurfaceVariant
        )

        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .background(colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
            placeholder = { Text("Пошук чатів...", color = colorScheme.onSurfaceVariant) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    chat: Chat,
    nickname: String? = null,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(10.dp),  // Внутренний padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = chat.avatarUrl,
            contentDescription = chat.username,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        // Chat info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            // Показуємо псевдонім якщо є, інакше оригінальне ім'я
            Text(
                text = nickname ?: chat.username ?: "Unknown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Якщо є псевдонім, показуємо оригінальне ім'я нижче
            if (nickname != null && chat.username != null) {
                Text(
                    text = "@${chat.username}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

                Text(
                    text = chat.lastMessage?.decryptedText ?: "Немає повідомлень",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Pulsing badge for unread messages
            if (chat.unreadCount > 0) {
                PulsingBadge(count = chat.unreadCount)
            }
        }
    }
}

@Composable
fun EmptyChatsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📭",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Немаєте чатів",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Почніть розмову зараз!",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun GroupItemRow(
    group: com.worldmates.messenger.data.model.Group,
    onClick: () -> Unit
) {
    ChatGlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)  // Компактнее
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),  // Меньше внутренний padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = group.avatarUrl,
                contentDescription = group.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Group info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = group.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    Text(
                        text = "${group.membersCount} членів",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (group.isPrivate) {
                        Text(
                            text = " • Приватна",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Admin badge
            if (group.isAdmin) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGroupsState(onCreateClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👥",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Немає груп",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Створіть нову групу або приєднайтесь до існуючої",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Велика кнопка створення групи
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0084FF)
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Створити групу",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    onDismiss: () -> Unit,
    onUserClick: (com.worldmates.messenger.network.SearchUser) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.worldmates.messenger.network.SearchUser>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пошук користувачів") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Search field
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть ім'я або username") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search button
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true
                            errorMessage = null
                            // Perform search
                            coroutineScope.launch {
                                try {
                                    val response = com.worldmates.messenger.network.RetrofitClient.apiService.searchUsers(
                                        accessToken = com.worldmates.messenger.data.UserSession.accessToken ?: "",
                                        query = searchQuery
                                    )
                                    if (response.apiStatus == 200 && response.users != null) {
                                        searchResults = response.users
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Нічого не знайдено"
                                        searchResults = emptyList()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Помилка: ${e.localizedMessage}"
                                    searchResults = emptyList()
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching && searchQuery.isNotBlank()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Шукати")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Search results
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(user) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = user.name ?: user.username,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${user.username}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            if (user.verified == 1) {
                                Text("✓", color = Color(0xFF0084FF), fontSize = 20.sp)
                            }
                        }
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

/**
 * Діалог для перейменування контакту (встановлення локального псевдоніма)
 */
@Composable
fun RenameContactDialog(
    chat: Chat,
    currentNickname: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    nicknameRepository: ContactNicknameRepository
) {
    val scope = rememberCoroutineScope()
    val existingNickname by nicknameRepository.getNickname(chat.userId).collectAsState(initial = null)
    var nickname by remember { mutableStateOf(existingNickname ?: "") }

    // Оновлюємо nickname, коли existingNickname змінюється
    LaunchedEffect(existingNickname) {
        if (existingNickname != null) {
            nickname = existingNickname
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Перейменувати контакт")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Встановіть зручне ім'я для контакту ${chat.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Псевдонім") },
                    placeholder = { Text(chat.username ?: "Введіть псевдонім") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (existingNickname != null) {
                    TextButton(
                        onClick = {
                            nickname = ""
                            onSave(null)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Скинути до оригінального імені")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(if (nickname.isBlank()) null else nickname.trim())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
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