package com.worldmates.messenger.ui.chats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.worldmates.messenger.data.ContactNicknameRepository
import com.worldmates.messenger.data.model.Chat
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.ui.preferences.UIStyle
import com.worldmates.messenger.ui.preferences.rememberUIStyle
import com.worldmates.messenger.ui.theme.ExpressiveFAB
import com.worldmates.messenger.ui.theme.ExpressiveIconButton
import com.worldmates.messenger.ui.theme.GlassTopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Сучасний екран чатів з:
 * - HorizontalPager для свайпу між вкладками
 * - Pull-to-Refresh на кожній вкладці
 * - Автооновлення кожні 6 секунд
 * - Вибір між WorldMates та Telegram стилем
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class)
@Composable
fun ChatsScreenModern(
    viewModel: ChatsViewModel,
    groupsViewModel: com.worldmates.messenger.ui.groups.GroupsViewModel,
    onChatClick: (Chat) -> Unit,
    onGroupClick: (Group) -> Unit,
    onSettingsClick: () -> Unit
) {
    val chats by viewModel.chatList.collectAsState()
    val groups by groupsViewModel.groupList.collectAsState()
    val isLoadingChats by viewModel.isLoading.collectAsState()
    val isLoadingGroups by groupsViewModel.isLoading.collectAsState()
    val availableUsers by groupsViewModel.availableUsers.collectAsState()
    val isCreatingGroup by groupsViewModel.isCreatingGroup.collectAsState()

    val uiStyle = rememberUIStyle()
    val pagerState = rememberPagerState(initialPage = 0) { 2 } // 2 вкладки: Чати, Групи
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Стан для бічної панелі налаштувань
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Автооновлення кожні 6 секунд
    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(6000) // 6 секунд
            if (pagerState.currentPage == 0) {
                viewModel.fetchChats()
            } else {
                groupsViewModel.fetchGroups()
            }
        }
    }

    // Load available users when switching to groups tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            groupsViewModel.loadAvailableUsers()
        }
    }

    // Стан для діалогів
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showContactMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    // 📇 Стан для ContactPicker
    var showContactPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val nicknameRepository = remember { ContactNicknameRepository(context) }

    // ModalNavigationDrawer для свайпу налаштувань
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = androidx.compose.ui.Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Контент бічної панелі налаштувань
                SettingsDrawerContent(
                    onNavigateToFullSettings = {
                        scope.launch {
                            drawerState.close()
                        }
                        onSettingsClick()
                    },
                    onClose = {
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onShowContactPicker = {
                        showContactPicker = true
                    },
                    onShowDrafts = {
                        // Открываем экран черновиков
                        context.startActivity(
                            android.content.Intent(context, com.worldmates.messenger.ui.drafts.DraftsActivity::class.java)
                        )
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassTopAppBar(
                title = {
                    Text(
                        text = if (pagerState.currentPage == 0) "Чати" else "Групи",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    // Кнопка меню для відкриття drawer
                    ExpressiveIconButton(onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                            contentDescription = "Меню"
                        )
                    }
                },
                actions = {
                    // Пошук користувачів/груп
                    ExpressiveIconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Пошук")
                    }
                    // Налаштування (залишаємо для швидкого доступу)
                    ExpressiveIconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB для створення групи (тільки на вкладці "Групи")
            if (pagerState.currentPage == 1) {
                ExpressiveFAB(
                    onClick = { showCreateGroupDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Створити групу")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow для перемикання між вкладками
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text("Чати") },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                )

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text("Групи") },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) }
                )
            }

            // HorizontalPager з вкладками
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // Вкладка "Чати" з pull-to-refresh
                        ChatListTab(
                            chats = chats,
                            isLoading = isLoadingChats,
                            uiStyle = uiStyle,
                            onRefresh = { viewModel.fetchChats() },
                            onChatClick = onChatClick,
                            onChatLongPress = { chat ->
                                selectedChat = chat
                                showContactMenu = true
                            }
                        )
                    }
                    1 -> {
                        // Вкладка "Групи" з pull-to-refresh
                        GroupListTab(
                            groups = groups,
                            isLoading = isLoadingGroups,
                            uiStyle = uiStyle,
                            onRefresh = { groupsViewModel.fetchGroups() },
                            onGroupClick = onGroupClick,
                            onGroupLongPress = { group ->
                                selectedGroup = group
                                showEditGroupDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Діалоги
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

    // Edit Group Dialog
    if (showEditGroupDialog && selectedGroup != null) {
        com.worldmates.messenger.ui.groups.EditGroupDialog(
            group = selectedGroup!!,
            onDismiss = {
                showEditGroupDialog = false
                selectedGroup = null
            },
            onUpdate = { newName ->
                groupsViewModel.updateGroup(
                    groupId = selectedGroup!!.id,
                    name = newName,
                    onSuccess = {
                        showEditGroupDialog = false
                        selectedGroup = null
                        groupsViewModel.fetchGroups()
                    }
                )
            },
            onDelete = {
                groupsViewModel.deleteGroup(
                    groupId = selectedGroup!!.id,
                    onSuccess = {
                        showEditGroupDialog = false
                        selectedGroup = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Групу видалено",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            },
            onUploadAvatar = { uri ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Завантаження аватарок буде реалізовано пізніше",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            isLoading = groupsViewModel.isLoading.collectAsState().value
        )
    }

    // User Search Dialog
    if (showSearchDialog) {
        UserSearchDialogForChats(
            onDismiss = { showSearchDialog = false },
            onUserClick = { user ->
                showSearchDialog = false
                onChatClick(
                    Chat(
                        id = 0,
                        userId = user.userId,
                        username = user.username,
                        avatarUrl = user.avatarUrl,
                        lastMessage = null,
                        unreadCount = 0
                    )
                )
            }
        )
    }

    // Contact Context Menu
    if (showContactMenu && selectedChat != null) {
        ContactContextMenu(
            chat = selectedChat!!,
            onDismiss = {
                showContactMenu = false
                selectedChat = null
            },
            onRename = { chat: Chat ->
                // Діалог відкривається всередині ContactContextMenu
            },
            onDelete = { chat: Chat ->
                showContactMenu = false
                scope.launch {
                    viewModel.hideChat(chat.userId)
                    snackbarHostState.showSnackbar(
                        message = "Чат приховано",
                        duration = SnackbarDuration.Short
                    )
                }
                selectedChat = null
            },
            nicknameRepository = nicknameRepository
        )
    }

    // 📇 ContactPicker для выбора контакта из телефонной книги
    if (showContactPicker) {
        com.worldmates.messenger.ui.components.ContactPicker(
            onContactSelected = { contact ->
                // Здесь можно добавить логику - например, открыть чат с контактом
                android.widget.Toast.makeText(
                    context,
                    "Выбран контакт: ${contact.name}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showContactPicker = false
            },
            onDismiss = {
                showContactPicker = false
            }
        )
    }
    }  // Закриваємо ModalNavigationDrawer
}

/**
 * Вкладка зі списком чатів та pull-to-refresh
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatListTab(
    chats: List<Chat>,
    isLoading: Boolean,
    uiStyle: UIStyle,
    onRefresh: () -> Unit,
    onChatClick: (Chat) -> Unit,
    onChatLongPress: (Chat) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (chats.isEmpty() && !isLoading) {
            // Порожній стан
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Немає чатів",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(chats, key = { it.id }) { chat ->
                    // Користувач може вибрати стиль в налаштуваннях
                    when (uiStyle) {
                        UIStyle.TELEGRAM -> {
                            TelegramChatItem(
                                chat = chat,
                                onClick = { onChatClick(chat) },
                                onLongPress = { onChatLongPress(chat) }
                            )
                        }
                        UIStyle.WORLDMATES -> {
                            ModernChatCard(
                                chat = chat,
                                onClick = { onChatClick(chat) },
                                onLongPress = { onChatLongPress(chat) }
                            )
                        }
                    }
                }
            }
        }

        // Pull-to-refresh індикатор
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Вкладка зі списком груп та pull-to-refresh
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GroupListTab(
    groups: List<Group>,
    isLoading: Boolean,
    uiStyle: UIStyle,
    onRefresh: () -> Unit,
    onGroupClick: (Group) -> Unit,
    onGroupLongPress: (Group) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (groups.isEmpty() && !isLoading) {
            // Порожній стан
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Немає груп",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(groups, key = { it.id }) { group ->
                    // Користувач може вибрати стиль в налаштуваннях
                    when (uiStyle) {
                        UIStyle.TELEGRAM -> {
                            TelegramGroupItem(
                                group = group,
                                onClick = { onGroupClick(group) },
                                onLongPress = { onGroupLongPress(group) }
                            )
                        }
                        UIStyle.WORLDMATES -> {
                            ModernGroupCard(
                                group = group,
                                onClick = { onGroupClick(group) },
                                onLongPress = { onGroupLongPress(group) }
                            )
                        }
                    }
                }
            }
        }

        // Pull-to-refresh індикатор
        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialogForChats(
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
                    onValueChange = { 
                        searchQuery = it
                        if (it.length >= 2) {
                            coroutineScope.launch {
                                isSearching = true
                                errorMessage = null
                                try {
                                    val response = com.worldmates.messenger.network.RetrofitClient.apiService.searchUsers(
                                        accessToken = com.worldmates.messenger.data.UserSession.accessToken ?: "",
                                        query = it,
                                        limit = 20
                                    )
                                    searchResults = response.users ?: emptyList()
                                } catch (e: Exception) {
                                    errorMessage = "Помилка пошуку: ${e.message}"
                                } finally {
                                    isSearching = false
                                }
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    placeholder = { Text("Введіть ім'я або username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading indicator
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // Error message
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Search results
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { user ->
                        ListItem(
                            headlineContent = { Text(user.name ?: user.username) },
                            supportingContent = { Text("@${user.username}") },
                            leadingContent = {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = user.username,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            modifier = Modifier.clickable { onUserClick(user) }
                        )
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
