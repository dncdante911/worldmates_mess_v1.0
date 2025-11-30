package com.worldmates.messenger.ui.messages

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.ui.theme.WorldMatesTheme
import com.worldmates.messenger.utils.DecryptionUtility
import org.json.JSONObject

class MessagesActivity : AppCompatActivity() {
    
    private lateinit var viewModel: MessagesViewModel
    private var recipientId: Long = 0
    private var recipientName: String = ""
    private var recipientAvatar: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Отримуємо ID одержувача з intent
        recipientId = intent.getLongExtra("recipient_id", 0)
        recipientName = intent.getStringExtra("recipient_name") ?: "Unknown"
        recipientAvatar = intent.getStringExtra("recipient_avatar") ?: ""
        
        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)
        viewModel.initialize(recipientId)
        
        setContent {
            WorldMatesTheme {
                MessagesScreen(
                    viewModel = viewModel,
                    recipientName = recipientName,
                    recipientAvatar = recipientAvatar,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    recipientName: String,
    recipientAvatar: String,
    onBackPressed: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        MessagesHeader(
            recipientName = recipientName,
            recipientAvatar = recipientAvatar,
            onBackPressed = onBackPressed
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
            }
        }

        // Input Field
        MessageInput(
            messageText = messageText,
            onMessageChange = { messageText = it },
            onSendClick = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
fun MessagesHeader(
    recipientName: String,
    recipientAvatar: String,
    onBackPressed: () -> Unit
) {
    TopAppBar(
        title = { Text(recipientName) },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0084FF)
        )
    )
}

@Composable
fun MessageBubble(message: Message) {
    val isOwn = message.fromId == com.worldmates.messenger.data.UserSession.userId
    val bgColor = if (isOwn) Color(0xFF0084FF) else Color.White
    val textColor = if (isOwn) Color.White else Color.Black
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = bgColor
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.decryptedText ?: "(Помилка дешифрування)",
                    color = textColor,
                    fontSize = 14.sp
                )
                
                // Якщо є медіа
                if (!message.mediaUrl.isNullOrEmpty()) {
                    Text(
                        text = "📎 Медіа-файл",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Text(
                    text = formatTime(message.timeStamp),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Введіть повідомлення...") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendClick() }),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        IconButton(
            onClick = onSendClick,
            enabled = !isLoading && messageText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (isLoading) Color.Gray else Color(0xFF0084FF)
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}