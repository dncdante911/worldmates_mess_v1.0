package com.worldmates.messenger.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.ui.login.LoginActivity
import com.worldmates.messenger.ui.theme.WorldMatesTheme

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        setContent {
            WorldMatesTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onLogout = {
                        UserSession.clearSession()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackPressed: () -> Unit,
    onLogout: () -> Unit
) {
    val username = UserSession.username ?: "Користувач"
    val avatar = UserSession.avatar
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        TopAppBar(
            title = { Text("Налаштування") },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0084FF),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Section
            item {
                ProfileSection(
                    username = username,
                    userId = UserSession.userId,
                    avatar = avatar,
                    onEditProfile = { /* TODO: Navigate to edit profile */ }
                )
            }

            // Settings sections
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Account Settings
            item {
                SettingsSection(title = "Акаунт")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Редагувати профіль",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Конфіденційність",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Сповіщення",
                    onClick = { /* TODO */ }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Chat Settings
            item {
                SettingsSection(title = "Чати")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Chat,
                    title = "Фон чату",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Сховище даних",
                    onClick = { /* TODO */ }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // App Settings
            item {
                SettingsSection(title = "Додаток")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Мова",
                    subtitle = "Українська",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Тема",
                    subtitle = "Світла",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Про додаток",
                    subtitle = "Версія 1.0.0",
                    onClick = { /* TODO */ }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Logout
            item {
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Вийти",
                    textColor = Color.Red,
                    onClick = { showLogoutDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Вийти з акаунту?") },
            text = { Text("Ви впевнені, що хочете вийти?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Вийти", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
fun ProfileSection(
    username: String,
    userId: Long,
    avatar: String?,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditProfile() },
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = avatar ?: "https://worldmates.club/upload/photos/d-avatar.jpg",
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // User info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "ID: $userId",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Edit icon
            Icon(
                Icons.Default.Edit,
                contentDescription = "Редагувати",
                tint = Color(0xFF0084FF)
            )
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0084FF),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (textColor == Color.Red) Color.Red else Color(0xFF0084FF),
                modifier = Modifier.size(24.dp)
            )

            // Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = textColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Arrow
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Перейти",
                tint = Color.Gray
            )
        }
    }
}
