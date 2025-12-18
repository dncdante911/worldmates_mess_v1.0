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
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.ThemeSettingsScreen
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем ThemeManager
        ThemeManager.initialize(this)

        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        // Загрузить данные пользователя при открытии настроек
        viewModel.fetchUserData()

        setContent {
            var currentScreen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Main) }

            WorldMatesThemedApp {
                when (currentScreen) {
                    SettingsScreen.Main -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBackPressed = { finish() },
                            onNavigate = { screen -> currentScreen = screen },
                            onLogout = {
                                UserSession.clearSession()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finishAffinity()
                            }
                        )
                    }
                    SettingsScreen.EditProfile -> {
                        EditProfileScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = SettingsScreen.Main }
                        )
                    }
                    SettingsScreen.Privacy -> {
                        PrivacySettingsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = SettingsScreen.Main }
                        )
                    }
                    SettingsScreen.Notifications -> {
                        NotificationSettingsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = SettingsScreen.Main }
                        )
                    }
                    SettingsScreen.Theme -> {
                        ThemeSettingsScreen(
                            onBackClick = { currentScreen = SettingsScreen.Main }
                        )
                    }
                    SettingsScreen.MyGroups -> {
                        MyGroupsScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = SettingsScreen.Main }
                        )
                    }
                }
            }
        }
    }
}

sealed class SettingsScreen {
    object Main : SettingsScreen()
    object EditProfile : SettingsScreen()
    object Privacy : SettingsScreen()
    object Notifications : SettingsScreen()
    object Theme : SettingsScreen()
    object MyGroups : SettingsScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackPressed: () -> Unit,
    onNavigate: (SettingsScreen) -> Unit,
    onLogout: () -> Unit
) {
    val username = UserSession.username ?: "Користувач"
    val avatar = UserSession.avatar
    val userData by viewModel.userData.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Показать сообщение об успехе
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccess()
        }
    }

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
            // Success Message
            if (successMessage != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // Profile Section
            item {
                ProfileSection(
                    username = username,
                    userId = UserSession.userId,
                    avatar = avatar,
                    email = userData?.email,
                    about = userData?.about,
                    onEditProfile = { onNavigate(SettingsScreen.EditProfile) }
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
                    subtitle = "Змінити особисті дані",
                    onClick = { onNavigate(SettingsScreen.EditProfile) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Конфіденційність",
                    subtitle = "Налаштування приватності",
                    onClick = { onNavigate(SettingsScreen.Privacy) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Сповіщення",
                    subtitle = "Керування сповіщеннями",
                    onClick = { onNavigate(SettingsScreen.Notifications) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Social Settings
            item {
                SettingsSection(title = "Соціальне")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Group,
                    title = "Мої групи",
                    subtitle = "${userData?.groupsCount ?: "0"} груп",
                    onClick = { onNavigate(SettingsScreen.MyGroups) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.People,
                    title = "Підписники",
                    subtitle = "${userData?.followersCount ?: "0"} підписників",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Підписки",
                    subtitle = "${userData?.followingCount ?: "0"} підписок",
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
                    subtitle = "Налаштувати фон чату",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Сховище даних",
                    subtitle = "Керування медіа файлами",
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
                    subtitle = userData?.language ?: "Українська",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Тема",
                    subtitle = "Налаштування кольорів",
                    onClick = { onNavigate(SettingsScreen.Theme) }
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

            // Account Info (if Pro)
            if (userData?.isPro == 1) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFD700)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "PRO акаунт",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "Дякуємо за підтримку!",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
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
    email: String?,
    about: String?,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditProfile() },
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
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
                    if (!email.isNullOrEmpty()) {
                        Text(
                            text = email,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Edit icon
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Редагувати",
                    tint = Color(0xFF0084FF)
                )
            }

            // About section
            if (!about.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = about,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
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
            if (textColor != Color.Red) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Перейти",
                    tint = Color.Gray
                )
            }
        }
    }
}
