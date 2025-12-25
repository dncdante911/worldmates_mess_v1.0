package com.worldmates.messenger.ui.chats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // Contact Context Menu
    if (showContactMenu && selectedChat != null) {
        ContactContextMenu(
            chat = selectedChat!!,
            onDismiss = {
                showContactMenu = false
                selectedChat = null
            },
            onRename = { chat ->
                // Діалог відкривається всередині ContactContextMenu
            },
            onDelete = { chat ->
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

/**
 * Контент бічної панелі налаштувань
 */
@Composable
fun SettingsDrawerContent(
    onNavigateToFullSettings: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
    ) {
        // Header з інфо користувача
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil.compose.AsyncImage(
                        model = com.worldmates.messenger.data.UserSession.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = com.worldmates.messenger.data.UserSession.username ?: "Користувач",
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "+380 (93) 025 39 41",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Закрити",
                        tint = Color.White
                    )
                }
            }
        }

        // Меню items
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Person,
                    title = "Мій профіль",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Мій профіль", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Group,
                    title = "Нова група",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Створити групу", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Chat,
                    title = "Контакти",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Контакти", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Call,
                    title = "Дзвінки",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Дзвінки", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Star,
                    title = "Збережені повідомлення",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Збережені", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Divider(
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Settings,
                    title = "Налаштування",
                    onClick = onNavigateToFullSettings
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Share,
                    title = "Запросити друзів",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "Запросити друзів", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                DrawerMenuItem(
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    title = "Про додаток",
                    onClick = {
                        onClose()
                        android.widget.Toast.makeText(context, "WorldMates Messenger v1.0", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/**
 * Елемент меню в бічній панелі
 */
@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

/**
 * Контекстне меню для чату
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactContextMenu(
    chat: Chat,
    onDismiss: () -> Unit,
    onRename: (Chat) -> Unit,
    onDelete: (Chat) -> Unit,
    nicknameRepository: ContactNicknameRepository
) {
    val sheetState = rememberModalBottomSheetState()
    val colorScheme = MaterialTheme.colorScheme
    val existingNickname by nicknameRepository.getNickname(chat.userId).collectAsState(initial = null)
    var showRenameDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // Заголовок
            Text(
                text = "Дії з контактом",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = colorScheme.onSurface
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Rename
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showRenameDialog = true
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Перейменувати",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (existingNickname != null) "Змінити псевдонім" else "Додати псевдонім",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface
                )
            }

            // Delete
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete(chat) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Видалити",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Приховати чат",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Inline Rename Dialog
    if (showRenameDialog) {
        RenameContactDialog(
            chat = chat,
            currentNickname = existingNickname,
            onDismiss = {
                showRenameDialog = false
                onDismiss()
            },
            onSave = { nickname ->
                scope.launch {
                    nicknameRepository.setNickname(chat.userId, nickname)
                    showRenameDialog = false
                    onDismiss()
                }
            },
            nicknameRepository = nicknameRepository
        )
    }
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
        existingNickname?.let {
            nickname = it
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
