package com.worldmates.messenger.ui.groups

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.data.model.GroupMember
import com.worldmates.messenger.ui.theme.WorldMatesTheme
import java.text.SimpleDateFormat
import java.util.*

class GroupDetailsActivity : AppCompatActivity() {

    private lateinit var viewModel: GroupsViewModel
    private var groupId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupId = intent.getLongExtra("group_id", 0)
        if (groupId == 0L) {
            finish()
            return
        }

        viewModel = ViewModelProvider(this).get(GroupsViewModel::class.java)

        setContent {
            WorldMatesTheme {
                GroupDetailsScreen(
                    groupId = groupId,
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onNavigateToMessages = {
                        // Already in messages, just go back
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupId: Long,
    viewModel: GroupsViewModel,
    onBackPressed: () -> Unit,
    onNavigateToMessages: () -> Unit
) {
    val groups by viewModel.groupList.collectAsState()
    val members by viewModel.groupMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()

    val group = groups.find { it.id == groupId }
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<GroupMember?>(null) }
    var showMemberOptionsMenu by remember { mutableStateOf(false) }

    // Load group members when screen opens
    LaunchedEffect(groupId) {
        viewModel.selectGroup(group ?: return@LaunchedEffect)
        viewModel.loadAvailableUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі групи") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0084FF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Група не знайдена")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // Header Section - Avatar, Name, Members Count
            item {
                GroupHeaderSection(
                    group = group,
                    onAvatarClick = {
                        if (group.isAdmin) {
                            showEditDialog = true
                        }
                    }
                )
            }

            // Action Buttons Row
            item {
                GroupActionButtons(
                    onSearchClick = { /* TODO: Implement search */ },
                    onNotificationsClick = { /* TODO: Toggle notifications */ },
                    onShareClick = { /* TODO: Share group */ }
                )
            }

            // Admin Controls Section (if user is admin)
            if (group.isAdmin) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AdminControlsSection(
                        onEditClick = { showEditDialog = true },
                        onAddMembersClick = { showAddMemberDialog = true }
                    )
                }
            }

            // Members Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                ) {
                    Column {
                        Text(
                            text = "Учасники • ${members.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Members List
            items(members.sortedByDescending { it.role == "admin" }) { member ->
                MemberCard(
                    member = member,
                    isCurrentUserAdmin = group.isAdmin,
                    onLongClick = {
                        if (group.isAdmin && member.userId != UserSession.userId) {
                            selectedMember = member
                            showMemberOptionsMenu = true
                        }
                    }
                )
            }

            // Settings & Actions Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                GroupActionsSection(
                    group = group,
                    onLeaveClick = { showLeaveConfirmation = true },
                    onDeleteClick = { showDeleteConfirmation = true }
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Edit Group Dialog
        if (showEditDialog) {
            EditGroupDialog(
                group = group,
                onDismiss = { showEditDialog = false },
                onUpdate = { newName ->
                    viewModel.updateGroup(group.id, newName)
                    showEditDialog = false
                },
                onDelete = {
                    viewModel.deleteGroup(group.id)
                    showDeleteConfirmation = false
                    onBackPressed()
                },
                onUploadAvatar = { uri ->
                    viewModel.uploadGroupAvatar(group.id, uri, context)
                },
                isLoading = isLoading
            )
        }

        // Add Member Dialog
        if (showAddMemberDialog) {
            AddMemberDialog(
                availableUsers = availableUsers.filter { user ->
                    members.none { it.userId == user.userId }
                },
                onDismiss = { showAddMemberDialog = false },
                onAddMember = { userId ->
                    viewModel.addGroupMember(group.id, userId)
                }
            )
        }

        // Member Options Bottom Sheet
        if (showMemberOptionsMenu && selectedMember != null) {
            MemberOptionsDialog(
                member = selectedMember!!,
                onDismiss = { showMemberOptionsMenu = false },
                onPromoteToAdmin = {
                    viewModel.setGroupRole(group.id, selectedMember!!.userId, "admin")
                    showMemberOptionsMenu = false
                },
                onDemoteToMember = {
                    viewModel.setGroupRole(group.id, selectedMember!!.userId, "member")
                    showMemberOptionsMenu = false
                },
                onRemove = {
                    viewModel.removeGroupMember(group.id, selectedMember!!.userId)
                    showMemberOptionsMenu = false
                }
            )
        }

        // Leave Group Confirmation
        if (showLeaveConfirmation) {
            AlertDialog(
                onDismissRequest = { showLeaveConfirmation = false },
                title = { Text("Вийти з групи?") },
                text = { Text("Ви впевнені, що хочете вийти з групи \"${group.name}\"?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.leaveGroup(group.id)
                            showLeaveConfirmation = false
                            onBackPressed()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Вийти")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveConfirmation = false }) {
                        Text("Скасувати")
                    }
                }
            )
        }

        // Delete Group Confirmation
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Видалити групу?") },
                text = { Text("Ви впевнені, що хочете видалити групу \"${group.name}\"? Цю дію неможливо скасувати.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGroup(group.id)
                            showDeleteConfirmation = false
                            onBackPressed()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Видалити")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Скасувати")
                    }
                }
            )
        }
    }
}

@Composable
fun GroupHeaderSection(
    group: Group,
    onAvatarClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Avatar
            AsyncImage(
                model = group.avatarUrl,
                contentDescription = "Group avatar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group Name
            Text(
                text = group.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Members Count
            Text(
                text = "${group.membersCount} учасників",
                fontSize = 14.sp,
                color = Color.Gray
            )

            // Description (if available)
            group.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupActionButtons(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.Search,
                label = "Пошук",
                onClick = onSearchClick
            )
            ActionButton(
                icon = Icons.Default.Notifications,
                label = "Сповіщення",
                onClick = onNotificationsClick
            )
            ActionButton(
                icon = Icons.Default.Share,
                label = "Поділитися",
                onClick = onShareClick
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFF0084FF),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun AdminControlsSection(
    onEditClick: () -> Unit,
    onAddMembersClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column {
            SettingsItem(
                icon = Icons.Default.Edit,
                title = "Редагувати групу",
                onClick = onEditClick
            )
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
            SettingsItem(
                icon = Icons.Default.PersonAdd,
                title = "Додати учасників",
                onClick = onAddMembersClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberCard(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
                    onLongClick = if (isCurrentUserAdmin) onLongClick else null
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = member.avatarUrl,
                contentDescription = member.username,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name and Role
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.username,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )

                    // Role Badge
                    if (member.role != "member") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (member.role) {
                                "admin" -> Color(0xFF0084FF)
                                "moderator" -> Color(0xFF4CAF50)
                                else -> Color.Gray
                            }
                        ) {
                            Text(
                                text = when (member.role) {
                                    "admin" -> "Адмін"
                                    "moderator" -> "Модератор"
                                    else -> ""
                                },
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Joined date
                val joinedDate = SimpleDateFormat("dd MMM yyyy", Locale("uk")).format(Date(member.joinedTime * 1000))
                Text(
                    text = "Приєднався $joinedDate",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(start = 76.dp))
    }
}

@Composable
fun GroupActionsSection(
    group: Group,
    onLeaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column {
            SettingsItem(
                icon = Icons.Default.ExitToApp,
                title = "Вийти з групи",
                titleColor = Color.Red,
                iconTint = Color.Red,
                onClick = onLeaveClick
            )

            if (group.isAdmin) {
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Видалити групу",
                    titleColor = Color.Red,
                    iconTint = Color.Red,
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Black,
    iconTint: Color = Color(0xFF0084FF),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    availableUsers: List<com.worldmates.messenger.network.SearchUser>,
    onDismiss: () -> Unit,
    onAddMember: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати учасника") },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                if (availableUsers.isEmpty()) {
                    item {
                        Text(
                            "Всі користувачі вже в групі",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(availableUsers) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddMember(user.userId)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.username, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
fun MemberOptionsDialog(
    member: GroupMember,
    onDismiss: () -> Unit,
    onPromoteToAdmin: () -> Unit,
    onDemoteToMember: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.username) },
        text = {
            Column {
                if (member.role == "member") {
                    TextButton(
                        onClick = onPromoteToAdmin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Призначити адміністратором", color = Color(0xFF0084FF))
                    }
                } else if (member.role == "admin") {
                    TextButton(
                        onClick = onDemoteToMember,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Зняти адміністратора", color = Color(0xFF0084FF))
                    }
                }

                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Видалити з групи", color = Color.Red)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
