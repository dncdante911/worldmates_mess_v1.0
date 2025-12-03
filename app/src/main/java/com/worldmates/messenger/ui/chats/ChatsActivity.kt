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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.worldmates.messenger.ui.messages.MessagesActivity
import com.worldmates.messenger.ui.theme.WorldMatesTheme

class ChatsActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatsViewModel
    private lateinit var groupsViewModel: com.worldmates.messenger.ui.groups.GroupsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChatsViewModel::class.java)
        groupsViewModel = ViewModelProvider(this).get(com.worldmates.messenger.ui.groups.GroupsViewModel::class.java)

        setContent {
            WorldMatesTheme {
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

    var searchText by remember { mutableStateOf("") }
    var showGroups by remember { mutableStateOf(false) }

    val filteredChats = chats.filter {
        it.username.contains(searchText, ignoreCase = true)
    }
    val filteredGroups = groups.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        TopAppBar(
            title = { Text("Повідомлення") },
            actions = {
                IconButton(onClick = { /* Додати новий чат */ }) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0084FF),
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
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
                    EmptyGroupsState()
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
                            ChatItemRow(
                                chat = chat,
                                onClick = { onChatClick(chat) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            modifier = Modifier.padding(horizontal = 12.dp),
            tint = Color.Gray
        )

        TextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp)),
            placeholder = { Text("Пошук чатів...") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp),
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
            Text(
                text = chat.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = chat.lastMessage?.decryptedText ?: "Немає повідомлень",
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Unread badge
        if (chat.unreadCount > 0) {
            Surface(
                modifier = Modifier
                    .size(24.dp),
                shape = CircleShape,
                color = Color(0xFF0084FF)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp),
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
                color = Color.Black
            )

            Row {
                Text(
                    text = "${group.membersCount} членів",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                if (group.isPrivate) {
                    Text(
                        text = " • Приватна",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Admin badge
        if (group.isAdmin) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = Color(0xFF0084FF)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "👤",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyGroupsState() {
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
    }
}