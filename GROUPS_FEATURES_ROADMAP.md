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

---

## 🚧 В розробці

### 2. Аватарки груп

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
