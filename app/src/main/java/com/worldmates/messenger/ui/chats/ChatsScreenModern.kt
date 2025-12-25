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
