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
import androidx.compose.material.icons.filled.Search
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

    val uiStyle = rememberUIStyle()
    val pagerState = rememberPagerState(initialPage = 0) { 2 } // 2 вкладки: Чати, Групи
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Стан для діалогів
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showContactMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val nicknameRepository = remember { ContactNicknameRepository(context) }

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
                actions = {
                    // Пошук
                    ExpressiveIconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Пошук")
                    }

                    // Налаштування
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

    // Діалоги (CreateGroupDialog, EditGroupDialog, etc.)
    // TODO: Додати діалоги
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
