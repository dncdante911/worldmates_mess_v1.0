# 📋 Roadmap функціоналу груп WorldMates

## ✅ Реалізовано

### 1. Базовий функціонал груп
- ✅ Створення груп
- ✅ Список груп
- ✅ Відправка/отримання повідомлень
- ✅ Додавання/видалення учасників
- ✅ **Редагування груп (назва)**
- ✅ **Видалення груп**

**Як використовувати:**
- Довге натискання на групу → відкриває діалог редагування
- Кнопка "Зберегти" → оновлює назву
- Кнопка "Видалити" → видаляє групу (з підтвердженням)

### 2. Аватарки груп
- ✅ Завантаження аватарок через EditGroupDialog
- ✅ Серверна валідація (тип файлу, розмір до 5MB)
- ✅ Автоматичне видалення старої аватарки
- ✅ Права доступу (тільки адміни)

**Як використовувати:**
- Відкрити діалог редагування групи
- Натиснути на іконку камери на аватарці
- Вибрати зображення з галереї
- Аватарка завантажується автоматично

### 3. Детальний екран групи (GroupDetailsActivity)
- ✅ Великий аватар групи, назва, опис
- ✅ Кількість учасників
- ✅ Кнопки дій (Пошук, Сповіщення, Поділитися)
- ✅ Список учасників з ролями (Admin/Moderator)
- ✅ Адмін-контроли (редагування, додавання учасників)
- ✅ Довге натискання на учасника → меню дій
- ✅ Вихід з групи / Видалення групи

**Навігація:**
- Клік на назву групи в чаті → відкриває GroupDetailsActivity

---

## 🚧 В розробці

### 4. Топіки/Теми в групах (як Telegram Topics)

**Концепція:**
Топіки дозволяють організувати обговорення в великих групах. Кожен топік - це окремий потік повідомлень з власною назвою та іконкою.

**Переваги:**
- Організація обговорень за темами
- Легше знайти потрібну інформацію
- Зменшення шуму в великих групах
- Можливість підписатися на конкретні топіки

#### Структура БД

##### Таблиця `Wo_GroupTopics`
```sql
CREATE TABLE IF NOT EXISTS Wo_GroupTopics (
    topic_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    topic_name VARCHAR(255) NOT NULL,
    topic_icon VARCHAR(50) DEFAULT '💬',
    topic_color VARCHAR(7) DEFAULT '#0084FF',
    created_by INT NOT NULL,
    created_time INT NOT NULL,
    is_general BOOLEAN DEFAULT FALSE,
    message_count INT DEFAULT 0,
    last_message_time INT DEFAULT NULL,
    FOREIGN KEY (group_id) REFERENCES Wo_GroupChat(group_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES Wo_Users(user_id) ON DELETE SET NULL,
    INDEX idx_group_id (group_id),
    INDEX idx_last_message (last_message_time)
);
```

##### Модифікація таблиці `Wo_Messages`
```sql
ALTER TABLE Wo_Messages ADD COLUMN topic_id INT DEFAULT NULL;
ALTER TABLE Wo_Messages ADD FOREIGN KEY (topic_id) REFERENCES Wo_GroupTopics(topic_id) ON DELETE SET NULL;
ALTER TABLE Wo_Messages ADD INDEX idx_topic_id (topic_id);
```

#### Server API Endpoints (group_chat_v2.php)

##### Створити топік
```php
case 'create_topic':
    $group_id = intval($_POST['group_id']);
    $topic_name = trim($_POST['topic_name']);
    $topic_icon = $_POST['topic_icon'] ?? '💬';
    $topic_color = $_POST['topic_color'] ?? '#0084FF';

    // Перевірка прав (тільки адміни можуть створювати топіки)
    $stmt = $db->prepare("SELECT user_id FROM Wo_GroupChat WHERE group_id = ?");
    $stmt->execute([$group_id]);
    $group = $stmt->fetch();

    if ($group['user_id'] != $current_user_id) {
        sendError(403, 'Only admins can create topics');
    }

    if (empty($topic_name)) {
        sendError(400, 'Topic name is required');
    }

    // Створюємо топік
    $stmt = $db->prepare("
        INSERT INTO Wo_GroupTopics (group_id, topic_name, topic_icon, topic_color, created_by, created_time)
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    $stmt->execute([$group_id, $topic_name, $topic_icon, $topic_color, $current_user_id, time()]);

    sendResponse(array(
        'api_status' => 200,
        'topic_id' => $db->lastInsertId(),
        'message' => 'Topic created successfully'
    ));
    break;
```

##### Отримати топіки групи
```php
case 'get_topics':
    $group_id = intval($_POST['group_id']);

    $stmt = $db->prepare("
        SELECT
            t.*,
            u.username as creator_name,
            u.avatar as creator_avatar,
            COUNT(DISTINCT m.id) as message_count
        FROM Wo_GroupTopics t
        LEFT JOIN Wo_Users u ON t.created_by = u.user_id
        LEFT JOIN Wo_Messages m ON m.topic_id = t.topic_id
        WHERE t.group_id = ?
        GROUP BY t.topic_id
        ORDER BY t.is_general DESC, t.last_message_time DESC
    ");
    $stmt->execute([$group_id]);
    $topics = $stmt->fetchAll();

    sendResponse(array(
        'api_status' => 200,
        'topics' => $topics
    ));
    break;
```

##### Отримати повідомлення топіка
```php
case 'get_topic_messages':
    $topic_id = intval($_POST['topic_id']);
    $limit = intval($_POST['limit'] ?? 50);
    $before_message_id = intval($_POST['before_message_id'] ?? 0);

    $where_clause = $before_message_id > 0
        ? "AND m.id < ?"
        : "";

    $stmt = $db->prepare("
        SELECT
            m.id,
            m.from_id,
            m.to_id,
            m.text,
            m.time,
            m.media,
            m.type,
            u.username as sender_name,
            u.avatar as sender_avatar
        FROM Wo_Messages m
        LEFT JOIN Wo_Users u ON m.from_id = u.user_id
        WHERE m.topic_id = ? $where_clause
        ORDER BY m.time DESC
        LIMIT ?
    ");

    if ($before_message_id > 0) {
        $stmt->execute([$topic_id, $before_message_id, $limit]);
    } else {
        $stmt->execute([$topic_id, $limit]);
    }

    $messages = $stmt->fetchAll();

    sendResponse(array(
        'api_status' => 200,
        'messages' => $messages
    ));
    break;
```

##### Надіслати повідомлення в топік
```php
case 'send_topic_message':
    $topic_id = intval($_POST['topic_id']);
    $text = trim($_POST['text']);

    // Отримуємо group_id з topic_id
    $stmt = $db->prepare("SELECT group_id FROM Wo_GroupTopics WHERE topic_id = ?");
    $stmt->execute([$topic_id]);
    $topic = $stmt->fetch();

    if (!$topic) {
        sendError(404, 'Topic not found');
    }

    // Вставляємо повідомлення
    $message_hash = uniqid('msg_');
    $time = time();

    $stmt = $db->prepare("
        INSERT INTO Wo_Messages (from_id, group_id, topic_id, text, message_hash_id, time)
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    $stmt->execute([$current_user_id, $topic['group_id'], $topic_id, $text, $message_hash, $time]);

    // Оновлюємо last_message_time топіка
    $stmt = $db->prepare("
        UPDATE Wo_GroupTopics
        SET last_message_time = ?, message_count = message_count + 1
        WHERE topic_id = ?
    ");
    $stmt->execute([$time, $topic_id]);

    sendResponse(array(
        'api_status' => 200,
        'message_id' => $db->lastInsertId(),
        'message' => 'Message sent successfully'
    ));
    break;
```

#### Android Models

```kotlin
data class Topic(
    @SerializedName("topic_id") val id: Long,
    @SerializedName("group_id") val groupId: Long,
    @SerializedName("topic_name") val name: String,
    @SerializedName("topic_icon") val icon: String = "💬",
    @SerializedName("topic_color") val color: String = "#0084FF",
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_time") val createdTime: Long,
    @SerializedName("is_general") val isGeneral: Boolean = false,
    @SerializedName("message_count") val messageCount: Int = 0,
    @SerializedName("last_message_time") val lastMessageTime: Long? = null,
    @SerializedName("creator_name") val creatorName: String? = null,
    @SerializedName("creator_avatar") val creatorAvatar: String? = null
)

data class TopicListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("topics") val topics: List<Topic>?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)
```

#### UI Implementation

##### TopicsTabRow (Horizontal scrollable tabs)
```kotlin
@Composable
fun TopicsTabRow(
    topics: List<Topic>,
    selectedTopicId: Long?,
    onTopicSelect: (Long) -> Unit,
    onCreateTopic: () -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = topics.indexOfFirst { it.id == selectedTopicId }.coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White,
        edgePadding = 8.dp
    ) {
        topics.forEach { topic ->
            Tab(
                selected = topic.id == selectedTopicId,
                onClick = { onTopicSelect(topic.id) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(topic.icon, fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(topic.name, fontSize = 14.sp)
                        if (topic.messageCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(android.graphics.Color.parseColor(topic.color))
                            ) {
                                Text(
                                    "${topic.messageCount}",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        // Create topic button
        Tab(
            selected = false,
            onClick = onCreateTopic,
            icon = {
                Icon(Icons.Default.Add, "Create topic", tint = Color(0xFF0084FF))
            }
        )
    }
}
```

##### CreateTopicDialog
```kotlin
@Composable
fun CreateTopicDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String, color: String) -> Unit
) {
    var topicName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💬") }
    var selectedColor by remember { mutableStateOf("#0084FF") }

    val iconOptions = listOf("💬", "📢", "📝", "💡", "🎯", "🔔", "📊", "🎨", "🎮", "📚")
    val colorOptions = listOf("#0084FF", "#FF4444", "#4CAF50", "#FFC107", "#9C27B0", "#00BCD4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Створити топік") },
        text = {
            Column {
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("Назва топіка") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text("Іконка:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconOptions) { icon ->
                        Surface(
                            shape = CircleShape,
                            color = if (icon == selectedIcon) Color(0xFFE3F2FD) else Color.Transparent,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedIcon = icon }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(icon, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Колір:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { color ->
                        Surface(
                            shape = CircleShape,
                            color = Color(android.graphics.Color.parseColor(color)),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (color == selectedColor) 3.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(topicName, selectedIcon, selectedColor) },
                enabled = topicName.isNotBlank()
            ) {
                Text("Створити")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}
```

---

### 2. Аватарки груп (ЗАВЕРШЕНО)

#### Сервер (group_chat_v2.php)
```php
case 'upload_avatar':
    // Endpoint для завантаження аватарки групи
    $group_id = $_POST['id'];

    // Перевірка прав (тільки адміни можуть змінювати аватарку)
    if (!isGroupAdmin($current_user_id, $group_id)) {
        sendError(403, 'Only admins can change group avatar');
    }

    // Завантаження файлу
    if (isset($_FILES['avatar'])) {
        $upload_dir = '../upload/photos/' . date('Y/m') . '/';
        if (!file_exists($upload_dir)) {
            mkdir($upload_dir, 0777, true);
        }

        $file_extension = pathinfo($_FILES['avatar']['name'], PATHINFO_EXTENSION);
        $new_filename = 'group_' . $group_id . '_' . time() . '.' . $file_extension;
        $upload_path = $upload_dir . $new_filename;

        if (move_uploaded_file($_FILES['avatar']['tmp_name'], $upload_path)) {
            // Оновлюємо avatar в БД
            $stmt = $db->prepare("UPDATE Wo_GroupChat SET avatar = ? WHERE group_id = ?");
            $stmt->execute([$upload_path, $group_id]);

            sendResponse(array(
                'api_status' => 200,
                'message' => 'Avatar uploaded successfully',
                'avatar_url' => $upload_path
            ));
        }
    }
    break;
```

#### Android (GroupsViewModel.kt)
```kotlin
fun uploadGroupAvatar(groupId: Long, imageUri: Uri) {
    viewModelScope.launch {
        try {
            val file = uriToFile(imageUri)
            val response = RetrofitClient.apiService.uploadGroupAvatar(
                accessToken = UserSession.accessToken!!,
                groupId = groupId,
                avatar = file
            )

            if (response.apiStatus == 200) {
                fetchGroups() // Оновити список
            }
        } catch (e: Exception) {
            _error.value = "Помилка завантаження аватарки: ${e.message}"
        }
    }
}
```

#### UI (EditGroupDialog.kt)
```kotlin
// Додати в EditGroupDialog:
var showImagePicker by remember { mutableStateOf(false) }
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let { onUploadAvatar(it) }
}

// Кнопка завантаження аватарки
OutlinedButton(
    onClick = { imagePickerLauncher.launch("image/*") }
) {
    Icon(Icons.Default.Camera, null)
    Spacer(Modifier.width(8.dp))
    Text("Змінити аватарку")
}
```

---

### 3. Підгрупи/Папки в групах

#### Структура БД
Додати таблицю `Wo_GroupFolders`:
```sql
CREATE TABLE Wo_GroupFolders (
    folder_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    parent_folder_id INT DEFAULT NULL,
    folder_name VARCHAR(255) NOT NULL,
    created_by INT NOT NULL,
    created_time INT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES Wo_GroupChat(group_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES Wo_GroupFolders(folder_id) ON DELETE CASCADE
);
```

Модифікувати `Wo_Messages` для прив'язки до папок:
```sql
ALTER TABLE Wo_Messages ADD COLUMN folder_id INT DEFAULT NULL;
ALTER TABLE Wo_Messages ADD FOREIGN KEY (folder_id) REFERENCES Wo_GroupFolders(folder_id) ON DELETE SET NULL;
```

#### API Endpoints (group_chat_v2.php)

##### Створити папку:
```php
case 'create_folder':
    $group_id = $_POST['group_id'];
    $folder_name = $_POST['folder_name'];
    $parent_folder_id = $_POST['parent_folder_id'] ?? null;

    $stmt = $db->prepare("
        INSERT INTO Wo_GroupFolders (group_id, parent_folder_id, folder_name, created_by, created_time)
        VALUES (?, ?, ?, ?, ?)
    ");
    $stmt->execute([$group_id, $parent_folder_id, $folder_name, $current_user_id, time()]);

    sendResponse(array(
        'api_status' => 200,
        'folder_id' => $db->lastInsertId()
    ));
    break;
```

##### Отримати папки групи:
```php
case 'get_folders':
    $group_id = $_POST['group_id'];

    $stmt = $db->prepare("
        SELECT f.*, u.username as creator_name,
               (SELECT COUNT(*) FROM Wo_Messages WHERE folder_id = f.folder_id) as message_count,
               (SELECT COUNT(*) FROM Wo_GroupFolders WHERE parent_folder_id = f.folder_id) as subfolder_count
        FROM Wo_GroupFolders f
        LEFT JOIN Wo_Users u ON f.created_by = u.user_id
        WHERE f.group_id = ? AND f.parent_folder_id IS NULL
        ORDER BY f.folder_name ASC
    ");
    $stmt->execute([$group_id]);
    $folders = $stmt->fetchAll();

    sendResponse(array(
        'api_status' => 200,
        'folders' => $folders
    ));
    break;
```

##### Отримати повідомлення папки:
```php
case 'get_folder_messages':
    $folder_id = $_POST['folder_id'];

    $stmt = $db->prepare("
        SELECT m.*, u.username, u.avatar
        FROM Wo_Messages m
        LEFT JOIN Wo_Users u ON m.from_id = u.user_id
        WHERE m.folder_id = ?
        ORDER BY m.time ASC
    ");
    $stmt->execute([$folder_id]);
    $messages = $stmt->fetchAll();

    sendResponse(array(
        'api_status' => 200,
        'messages' => $messages
    ));
    break;
```

#### Android Models
```kotlin
data class GroupFolder(
    @SerializedName("folder_id") val id: Long,
    @SerializedName("group_id") val groupId: Long,
    @SerializedName("parent_folder_id") val parentFolderId: Long?,
    @SerializedName("folder_name") val name: String,
    @SerializedName("created_by") val createdBy: Long,
    @SerializedName("created_time") val createdTime: Long,
    @SerializedName("message_count") val messageCount: Int = 0,
    @SerializedName("subfolder_count") val subfolderCount: Int = 0
)

data class FolderListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("folders") val folders: List<GroupFolder>?,
    @SerializedName("error_message") val errorMessage: String?
)
```

#### UI Structure
```
GroupMessagesActivity
├── TopBar: Назва групи
├── FolderNavigation: Хлібні крихти (Група > Папка1 > Папка2)
└── Content:
    ├── FoldersList: Список папок
    │   └── FolderCard
    │       ├── Icon(Folder)
    │       ├── Name
    │       ├── Counters (повідомлень/підпапок)
    │       └── onLongClick → EditFolder
    └── MessagesList: Повідомлення поточної папки
```

#### Приклад UI (GroupFoldersView.kt)
```kotlin
@Composable
fun GroupFoldersView(
    groupId: Long,
    currentFolderId: Long?,
    folders: List<GroupFolder>,
    onFolderClick: (GroupFolder) -> Unit,
    onCreateFolder: () -> Unit
) {
    Column {
        // Хлібні крихти
        FolderBreadcrumbs(path = getFolderPath(currentFolderId))

        // Список папок
        LazyColumn {
            items(folders) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { /* Меню редагування */ }
                )
            }
        }

        // FAB створення папки
        FloatingActionButton(onClick = onCreateFolder) {
            Icon(Icons.Default.CreateNewFolder, null)
        }
    }
}

@Composable
fun FolderCard(folder: GroupFolder, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFFC107)
            )

            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${folder.messageCount} повідомлень • ${folder.subfolderCount} папок",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
```

---

## 🎯 Пріоритети реалізації

1. **Аватарки груп** (1-2 години)
   - Простіше реалізувати
   - Покращує UX
   - Використовує існуючу інфраструктуру завантаження файлів

2. **Підгрупи/Папки** (4-6 годин)
   - Складніше - потрібні зміни в БД
   - Нова ієрархічна структура
   - Складніший UI з навігацією
   - Але дуже корисний функціонал для організації

---

## 📝 Примітки

- Всі backend методи для редагування/видалення вже існують у `group_chat_v2.php`
- Всі ViewModel методи вже існують у `GroupsViewModel.kt`
- Підгрупи можна зробити необмежено вкладеними (рекурсивна структура)
- Можна додати права доступу до папок (тільки адміни можуть створювати папки, тощо)

---

**Автор:** Claude Code
**Дата:** 2025-12-11
**Версія:** 1.0
