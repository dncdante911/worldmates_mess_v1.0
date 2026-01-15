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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Label
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
import com.worldmates.messenger.ui.channels.ChannelCard
import com.worldmates.messenger.ui.preferences.UIStyle
import com.worldmates.messenger.ui.preferences.rememberUIStyle
import com.worldmates.messenger.ui.theme.ExpressiveFAB
import com.worldmates.messenger.ui.theme.ExpressiveIconButton
import com.worldmates.messenger.ui.theme.GlassTopAppBar
import com.worldmates.messenger.ui.theme.rememberThemeState
import com.worldmates.messenger.ui.theme.BackgroundImage
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
    channelsViewModel: com.worldmates.messenger.ui.channels.ChannelsViewModel,
    storyViewModel: com.worldmates.messenger.ui.stories.StoryViewModel,
    onChatClick: (Chat) -> Unit,
    onGroupClick: (Group) -> Unit,
    onChannelClick: (com.worldmates.messenger.data.model.Channel) -> Unit,
    onSettingsClick: () -> Unit,
    onCreateChannelClick: () -> Unit = {}
) {
    val chats by viewModel.chatList.collectAsState()
    val groups by groupsViewModel.groupList.collectAsState()
    val channels by channelsViewModel.subscribedChannels.collectAsState()
    val isLoadingChats by viewModel.isLoading.collectAsState()
    val isLoadingGroups by groupsViewModel.isLoading.collectAsState()
    val isLoadingChannels by channelsViewModel.isLoading.collectAsState()
    val availableUsers by groupsViewModel.availableUsers.collectAsState()
    val isCreatingGroup by groupsViewModel.isCreatingGroup.collectAsState()

    // Stories state
    val stories by storyViewModel.stories.collectAsState()
    val isLoadingStories by storyViewModel.isLoading.collectAsState()

    val uiStyle = rememberUIStyle()
    val themeState = rememberThemeState()
    val pagerState = rememberPagerState(initialPage = 0) { 3 } // 3 вкладки: Чати, Канали, Групи
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Стан для бічної панелі налаштувань
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Автооновлення кожні 6 секунд
    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(6000) // 6 секунд
            when (pagerState.currentPage) {
                0 -> viewModel.fetchChats()
                1 -> channelsViewModel.fetchSubscribedChannels()
                2 -> groupsViewModel.fetchGroups()
            }
        }
    }

    // Load available users when switching to groups tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) {
            groupsViewModel.loadAvailableUsers()
        }
    }

    // Стан для діалогів
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
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
                    },
                    onCreateStoryClick = {
                        showCreateStoryDialog = true
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
    // Box з фоновим зображенням з налаштувань тем
    Box(modifier = Modifier.fillMaxSize()) {
        // Фонове зображення з налаштувань тем
        BackgroundImage(
            backgroundImageUri = themeState.backgroundImageUri,
            presetBackgroundId = themeState.presetBackgroundId
        )

        Scaffold(
            containerColor = Color.Transparent,  // Прозорий фон, щоб було видно BackgroundImage
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
            // FAB для створення каналу/групи
            when (pagerState.currentPage) {
                1 -> {
                    // Вкладка Канали - створити канал
                    ExpressiveFAB(
                        onClick = onCreateChannelClick
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Створити канал")
                    }
                }
                2 -> {
                    // Вкладка Групи - створити групу
                    ExpressiveFAB(
                        onClick = { showCreateGroupDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Створити групу")
                    }
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
                    text = { Text("Канали") },
                    icon = { Icon(Icons.Default.Label, contentDescription = null) }
                )

                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(2)
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
                        // Вкладка "Чати" з pull-to-refresh + Stories
                        ChatListTabWithStories(
                            chats = chats,
                            stories = stories,
                            isLoading = isLoadingChats,
                            isLoadingStories = isLoadingStories,
                            uiStyle = uiStyle,
                            onRefresh = {
                                viewModel.fetchChats()
                                storyViewModel.loadStories()
                            },
                            onChatClick = onChatClick,
                            onChatLongPress = { chat ->
                                selectedChat = chat
                                showContactMenu = true
                            },
                            onCreateStoryClick = {
                                showCreateStoryDialog = true
                            }
                        )
                    }
                    1 -> {
                        // Вкладка "Канали" з pull-to-refresh + Channel Stories
                        ChannelListTabWithStories(
                            channels = channels,
                            stories = stories,
                            isLoading = isLoadingChannels,
                            isLoadingStories = isLoadingStories,
                            uiStyle = uiStyle,
                            channelsViewModel = channelsViewModel,
                            onRefresh = {
                                channelsViewModel.fetchSubscribedChannels()
                                storyViewModel.loadStories()
                            },
                            onChannelClick = onChannelClick
                        )
                    }
                    2 -> {
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

    // Create Story Dialog
    if (showCreateStoryDialog) {
        com.worldmates.messenger.ui.stories.CreateStoryDialog(
            onDismiss = { showCreateStoryDialog = false },
            viewModel = storyViewModel
        )
    }
    }  // Закриваємо Box з фоновим зображенням
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
    val context = LocalContext.current
    val nicknameRepository = remember { ContactNicknameRepository(context) }
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
                    val nickname by nicknameRepository.getNickname(chat.userId).collectAsState(initial = null)

                    // Користувач може вибрати стиль в налаштуваннях
                    when (uiStyle) {
                        UIStyle.TELEGRAM -> {
                            TelegramChatItem(
                                chat = chat,
                                nickname = nickname,
                                onClick = { onChatClick(chat) },
                                onLongPress = { onChatLongPress(chat) }
                            )
                        }
                        UIStyle.WORLDMATES -> {
                            ModernChatCard(
                                chat = chat,
                                nickname = nickname,
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

/**
 * Вкладка зі списком каналів
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChannelListTab(
    channels: List<com.worldmates.messenger.data.model.Channel>,
    isLoading: Boolean,
    uiStyle: UIStyle,
    channelsViewModel: com.worldmates.messenger.ui.channels.ChannelsViewModel,
    onRefresh: () -> Unit,
    onChannelClick: (com.worldmates.messenger.data.model.Channel) -> Unit
) {
    val context = LocalContext.current
    val refreshing by remember { mutableStateOf(false) }
    val searchQuery by channelsViewModel.searchQuery.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onRefresh
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Пошукова панель
        var localQuery by remember { mutableStateOf(searchQuery) }

        // Автоматичний пошук з затримкою після введення
        LaunchedEffect(localQuery) {
            kotlinx.coroutines.delay(500) // Затримка 500мс після введення
            if (localQuery.isEmpty()) {
                channelsViewModel.fetchChannels()
            } else if (localQuery.length >= 2) {
                channelsViewModel.searchChannels(localQuery)
            }
        }

        // Автоматичне оновлення списку каналів кожні 20 секунд (тільки якщо не в режимі пошуку)
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(20000) // 20 секунд
                if (localQuery.isEmpty()) {
                    // Тихе оновлення без показу індикатора
                    channelsViewModel.fetchChannels()
                }
            }
        }

        com.worldmates.messenger.ui.channels.ChannelSearchBar(
            searchQuery = localQuery,
            onQueryChange = { query ->
                localQuery = query
            },
            onSearch = {
                if (localQuery.isNotEmpty()) {
                    channelsViewModel.searchChannels(localQuery)
                }
            },
            onClear = {
                localQuery = ""
                channelsViewModel.fetchChannels()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (channels.isEmpty() && !isLoading) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Gray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Немає каналів",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Підпишіться на канали, щоб бачити їх тут",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = channels,
                    key = { it.id }
                ) { channel ->
                    // Користувач може вибрати стиль в налаштуваннях
                    when (uiStyle) {
                        com.worldmates.messenger.ui.preferences.UIStyle.TELEGRAM -> {
                            com.worldmates.messenger.ui.channels.TelegramChannelItem(
                                channel = channel,
                                onClick = { onChannelClick(channel) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        com.worldmates.messenger.ui.preferences.UIStyle.WORLDMATES -> {
                            com.worldmates.messenger.ui.channels.ChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel) },
                                onSubscribeToggle = { isCurrentlySubscribed ->
                                    if (isCurrentlySubscribed) {
                                        channelsViewModel.unsubscribeChannel(
                                            channelId = channel.id,
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Відписано", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(context, "Помилка: $error", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        channelsViewModel.subscribeChannel(
                                            channelId = channel.id,
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Підписано!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(context, "Помилка: $error", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .animateItem()
                            )
                        }
                    }
                }
            }
        }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Вкладка зі списком чатів + Stories вгорі
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListTabWithStories(
    chats: List<Chat>,
    stories: List<com.worldmates.messenger.data.model.Story>,
    isLoading: Boolean,
    isLoadingStories: Boolean,
    uiStyle: UIStyle,
    onRefresh: () -> Unit,
    onChatClick: (Chat) -> Unit,
    onChatLongPress: (Chat) -> Unit = {},
    onCreateStoryClick: () -> Unit = {}
) {
    val refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing || isLoading,
        onRefresh = onRefresh
    )
    val context = LocalContext.current
    val nicknameRepository = remember { ContactNicknameRepository(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Stories row вгорі
            item {
                com.worldmates.messenger.ui.stories.PersonalStoriesRow(
                    stories = stories,
                    onCreateStoryClick = onCreateStoryClick
                )
            }

            // Чати
            items(chats, key = { it.id }) { chat ->
                val nickname by nicknameRepository.getNickname(chat.userId).collectAsState(initial = null)

                when (uiStyle) {
                    UIStyle.WORLDMATES -> {
                        ModernChatCard(
                            chat = chat,
                            nickname = nickname,
                            onClick = { onChatClick(chat) },
                            onLongPress = { onChatLongPress(chat) }
                        )
                    }
                    UIStyle.TELEGRAM -> {
                        TelegramChatItem(
                            chat = chat,
                            nickname = nickname,
                            onClick = { onChatClick(chat) },
                            onLongPress = { onChatLongPress(chat) }
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing || isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Вкладка з каналами + Channel Stories
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelListTabWithStories(
    channels: List<com.worldmates.messenger.data.model.Channel>,
    stories: List<com.worldmates.messenger.data.model.Story>,
    isLoading: Boolean,
    isLoadingStories: Boolean,
    uiStyle: UIStyle,
    channelsViewModel: com.worldmates.messenger.ui.channels.ChannelsViewModel,
    onRefresh: () -> Unit,
    onChannelClick: (com.worldmates.messenger.data.model.Channel) -> Unit
) {
    val context = LocalContext.current
    val refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing || isLoading,
        onRefresh = onRefresh
    )

    // Групуємо stories по каналах
    val channelStoriesMap = stories.groupBy { it.userId }
    val channelStoriesWithData = channels.mapNotNull { channel ->
        channelStoriesMap[channel.ownerId]?.let { channelStories ->
            Pair(channel, channelStories)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Channel Stories секція (якщо є)
            if (channelStoriesWithData.isNotEmpty()) {
                item {
                    com.worldmates.messenger.ui.stories.ChannelStoriesSection(
                        channelStories = channelStoriesWithData
                    )
                }
            }

            // Список каналів
            items(channels, key = { it.id }) { channel ->
                when (uiStyle) {
                    UIStyle.WORLDMATES -> {
                        com.worldmates.messenger.ui.channels.ChannelCard(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onSubscribeToggle = { isCurrentlySubscribed ->
                                if (isCurrentlySubscribed) {
                                    channelsViewModel.unsubscribeChannel(
                                        channelId = channel.id,
                                        onSuccess = {
                                            android.widget.Toast.makeText(context, "Відписано", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            android.widget.Toast.makeText(context, "Помилка: $error", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    channelsViewModel.subscribeChannel(
                                        channelId = channel.id,
                                        onSuccess = {
                                            android.widget.Toast.makeText(context, "Підписано!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            android.widget.Toast.makeText(context, "Помилка: $error", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .animateItem()
                        )
                    }
                    UIStyle.TELEGRAM -> {
                        // Telegram style - використовуємо TelegramChannelItem
                        com.worldmates.messenger.ui.channels.TelegramChannelItem(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing || isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
