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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChatsViewModel::class.java)

        setContent {
            WorldMatesTheme {
                ChatsScreen(
                    viewModel = viewModel,
                    onChatClick = { chat ->
                        navigateToMessages(chat)
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
}

@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel,
    onChatClick: (Chat) -> Unit
) {
    val chats by viewModel.chatList.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val filteredChats = chats.filter { 
        it.username.contains(searchText, ignoreCase = true) ||
        it.username.contains(searchText, ignoreCase = true)
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0084FF)
            )
        )

        // Search
        SearchBar(
            searchText = searchText,
            onSearchChange = { searchText = it }
        )

  Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { showGroups = false }) { Text("Чаты") }
        Button(onClick = { showGroups = true }) { Text("Групи") }
    }

if (showGroups) {
        // Show groups list
    } else {
        // Show chats list
    }

        // Chats List
        if (filteredChats.isEmpty()) {
            EmptyChatsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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