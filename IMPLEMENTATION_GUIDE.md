# 📚 Детальна інструкція по реалізації

## 🎯 Як користуватися цією інструкцією

В файлі `GROUPS_FEATURES_ROADMAP.md` є багато готового коду, але незрозуміло **куди** його вставляти.

**Ця інструкція показує ПОКРОКОВО:**
- Який файл відкрити
- Що знайти в файлі
- Куди вставити код
- Що замінити/додати

---

# 📱 Реалізація Топіків/Тем

## Крок 1: База даних (MariaDB)

### 1.1 Створення таблиці Wo_GroupTopics

**Де виконувати:** phpMyAdmin → worldmates.club → SQL вкладка

**Код для виконання:**

```sql
-- Спочатку перевіряємо чи таблиця не існує
DROP TABLE IF EXISTS Wo_GroupTopics;

-- Створюємо таблицю
CREATE TABLE Wo_GroupTopics (
    topic_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    group_id INT UNSIGNED NOT NULL,
    topic_name VARCHAR(255) NOT NULL,
    topic_icon VARCHAR(50) DEFAULT '💬',
    topic_color VARCHAR(7) DEFAULT '#0084FF',
    created_by INT UNSIGNED NOT NULL,
    created_time INT UNSIGNED NOT NULL,
    is_general TINYINT(1) DEFAULT 0,
    message_count INT UNSIGNED DEFAULT 0,
    last_message_time INT UNSIGNED DEFAULT NULL,
    INDEX idx_group_id (group_id),
    INDEX idx_last_message (last_message_time),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Перевірка:**
```sql
DESCRIBE Wo_GroupTopics;
```

Має показати 10 колонок.

---

### 1.2 Додавання topic_id в Wo_Messages

**Важливо:** Спочатку перевіряємо чи колонка вже існує!

```sql
-- Перевірка
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Wo_Messages'
  AND COLUMN_NAME = 'topic_id';
```

**Якщо результат порожній** - виконуємо:
```sql
ALTER TABLE Wo_Messages ADD COLUMN topic_id INT UNSIGNED DEFAULT NULL;
ALTER TABLE Wo_Messages ADD INDEX idx_topic_id (topic_id);
```

**Якщо результат показує колонку** - пропускаємо цей крок! ✅

---

## Крок 2: Server API (PHP)

### 2.1 Відкрити файл

**Шлях:** `/var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php`

або

**Локально:** `server_modifications/group_chat_v2.php`

---

### 2.2 Знайти switch ($type)

Пошукайте в файлі цей блок:

```php
$type = $_GET['type'] ?? '';
logMessage("Type: $type");

switch ($type) {
    case 'create':
        // ... існуючий код ...
        break;

    case 'get_list':
        // ... існуючий код ...
        break;

    // ... інші cases ...
```

---

### 2.3 Додати ПЕРЕД `default:` case

Знайдіть в кінці switch цей код:

```php
    default:
        sendError(400, 'Invalid type parameter');
}
```

**ПЕРЕД `default:`** додайте:

```php
    // ============================================
    // TOPICS ENDPOINTS
    // ============================================

    case 'create_topic':
        logMessage("--- CREATE TOPIC ---");

        $group_id = intval($_POST['group_id']);
        $topic_name = trim($_POST['topic_name']);
        $topic_icon = $_POST['topic_icon'] ?? '💬';
        $topic_color = $_POST['topic_color'] ?? '#0084FF';

        // Перевірка прав (тільки адміни)
        $stmt = $db->prepare("SELECT user_id FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        $group = $stmt->fetch();

        if (!$group || $group['user_id'] != $current_user_id) {
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

    case 'get_topics':
        logMessage("--- GET TOPICS ---");

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

    case 'get_topic_messages':
        logMessage("--- GET TOPIC MESSAGES ---");

        $topic_id = intval($_POST['topic_id']);
        $limit = intval($_POST['limit'] ?? 50);
        $before_message_id = intval($_POST['before_message_id'] ?? 0);

        $where_clause = $before_message_id > 0 ? "AND m.id < ?" : "";

        $stmt = $db->prepare("
            SELECT
                m.id,
                m.from_id,
                m.to_id,
                m.text,
                m.time,
                m.media,
                CASE
                    WHEN m.media != '' THEN 'media'
                    WHEN m.text LIKE 'http%' THEN 'media'
                    ELSE 'text'
                END as type,
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

    case 'send_topic_message':
        logMessage("--- SEND TOPIC MESSAGE ---");

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

    case 'update_topic':
        logMessage("--- UPDATE TOPIC ---");

        $topic_id = intval($_POST['topic_id']);
        $topic_name = trim($_POST['topic_name']);
        $topic_icon = $_POST['topic_icon'] ?? null;
        $topic_color = $_POST['topic_color'] ?? null;

        // Перевірка прав
        $stmt = $db->prepare("
            SELECT t.group_id, g.user_id
            FROM Wo_GroupTopics t
            JOIN Wo_GroupChat g ON t.group_id = g.group_id
            WHERE t.topic_id = ?
        ");
        $stmt->execute([$topic_id]);
        $topic = $stmt->fetch();

        if (!$topic || $topic['user_id'] != $current_user_id) {
            sendError(403, 'Only admins can update topics');
        }

        // Оновлюємо
        $updates = [];
        $params = [];

        if (!empty($topic_name)) {
            $updates[] = "topic_name = ?";
            $params[] = $topic_name;
        }
        if ($topic_icon !== null) {
            $updates[] = "topic_icon = ?";
            $params[] = $topic_icon;
        }
        if ($topic_color !== null) {
            $updates[] = "topic_color = ?";
            $params[] = $topic_color;
        }

        if (empty($updates)) {
            sendError(400, 'No fields to update');
        }

        $params[] = $topic_id;
        $sql = "UPDATE Wo_GroupTopics SET " . implode(', ', $updates) . " WHERE topic_id = ?";

        $stmt = $db->prepare($sql);
        $stmt->execute($params);

        sendResponse(array(
            'api_status' => 200,
            'message' => 'Topic updated successfully'
        ));
        break;

    case 'delete_topic':
        logMessage("--- DELETE TOPIC ---");

        $topic_id = intval($_POST['topic_id']);

        // Перевірка прав та чи це не General топік
        $stmt = $db->prepare("
            SELECT t.is_general, t.group_id, g.user_id
            FROM Wo_GroupTopics t
            JOIN Wo_GroupChat g ON t.group_id = g.group_id
            WHERE t.topic_id = ?
        ");
        $stmt->execute([$topic_id]);
        $topic = $stmt->fetch();

        if (!$topic) {
            sendError(404, 'Topic not found');
        }

        if ($topic['user_id'] != $current_user_id) {
            sendError(403, 'Only admins can delete topics');
        }

        if ($topic['is_general']) {
            sendError(400, 'Cannot delete general topic');
        }

        // Видаляємо топік
        $stmt = $db->prepare("DELETE FROM Wo_GroupTopics WHERE topic_id = ?");
        $stmt->execute([$topic_id]);

        // Опціонально: видалити всі повідомлення топіка
        // $stmt = $db->prepare("DELETE FROM Wo_Messages WHERE topic_id = ?");
        // $stmt->execute([$topic_id]);

        // АБО обнулити topic_id (залишити повідомлення)
        $stmt = $db->prepare("UPDATE Wo_Messages SET topic_id = NULL WHERE topic_id = ?");
        $stmt->execute([$topic_id]);

        sendResponse(array(
            'api_status' => 200,
            'message' => 'Topic deleted successfully'
        ));
        break;
```

**Збережіть файл!**

---

## Крок 3: Android Models

### 3.1 Відкрити файл

**Шлях:** `app/src/main/java/com/worldmates/messenger/data/model/Group.kt`

---

### 3.2 Знайти кінець файлу

Прокрутіть до самого кінця файлу, після всіх існуючих data class.

---

### 3.3 Додати ПЕРЕД останньою фігурною дужкою:

```kotlin
// ==================== TOPICS ====================

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

**Збережіть файл!**

---

## Крок 4: Android API

### 4.1 Відкрити файл

**Шлях:** `app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt`

---

### 4.2 Знайти interface WorldMatesApi

```kotlin
interface WorldMatesApi {

    @POST("/api/auth.php")
    @FormUrlEncoded
    suspend fun login(
        // ... існуючі методи ...
    )
```

---

### 4.3 Додати ПЕРЕД останньою фігурною дужкою interface:

```kotlin
    // ==================== TOPICS ====================

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun getTopics(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "get_topics",
        @Field("group_id") groupId: Long
    ): TopicListResponse

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun createTopic(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "create_topic",
        @Field("group_id") groupId: Long,
        @Field("topic_name") topicName: String,
        @Field("topic_icon") topicIcon: String,
        @Field("topic_color") topicColor: String
    ): CreateGroupResponse

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun getTopicMessages(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "get_topic_messages",
        @Field("topic_id") topicId: Long,
        @Field("limit") limit: Int = 50,
        @Field("before_message_id") beforeMessageId: Long = 0
    ): MessageListResponse

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun sendTopicMessage(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "send_topic_message",
        @Field("topic_id") topicId: Long,
        @Field("text") text: String
    ): MessageListResponse

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun updateTopic(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "update_topic",
        @Field("topic_id") topicId: Long,
        @Field("topic_name") topicName: String?,
        @Field("topic_icon") topicIcon: String?,
        @Field("topic_color") topicColor: String?
    ): CreateGroupResponse

    @POST("/api/v2/group_chat_v2.php")
    @FormUrlEncoded
    suspend fun deleteTopic(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "delete_topic",
        @Field("topic_id") topicId: Long
    ): CreateGroupResponse
```

**Збережіть файл!**

---

## Крок 5: ViewModel

### 5.1 Відкрити файл

**Шлях:** `app/src/main/java/com/worldmates/messenger/ui/groups/GroupsViewModel.kt`

---

### 5.2 Додати StateFlow для топіків

Знайдіть в класі блок де оголошені інші StateFlow:

```kotlin
class GroupsViewModel : ViewModel() {

    private val _groupList = MutableStateFlow<List<Group>>(emptyList())
    val groupList: StateFlow<List<Group>> = _groupList

    // ... інші StateFlow ...
```

**Після останнього StateFlow** додайте:

```kotlin
    // Topics
    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics

    private val _selectedTopic = MutableStateFlow<Topic?>(null)
    val selectedTopic: StateFlow<Topic?> = _selectedTopic
```

---

### 5.3 Додати методи для топіків

Знайдіть кінець класу (перед останньою `}`) і додайте:

```kotlin
    // ==================== TOPICS ====================

    fun loadTopics(groupId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getTopics(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId
                )

                if (response.apiStatus == 200 && response.topics != null) {
                    _topics.value = response.topics
                    _error.value = null
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося завантажити топіки"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка завантаження топіків", e)
            }
        }
    }

    fun createTopic(groupId: Long, name: String, icon: String, color: String) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.createTopic(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    topicName = name,
                    topicIcon = icon,
                    topicColor = color
                )

                if (response.apiStatus == 200) {
                    loadTopics(groupId) // Оновити список топіків
                    _error.value = null
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося створити топік"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка створення топіка", e)
            }
        }
    }

    fun selectTopic(topicId: Long?) {
        _selectedTopic.value = _topics.value.find { it.id == topicId }
    }

    fun loadTopicMessages(topicId: Long) {
        // TODO: завантажити повідомлення для топіка
        // Використовувати RetrofitClient.apiService.getTopicMessages()
    }
```

**Збережіть файл!**

---

## Крок 6: UI Components

### 6.1 Створити новий файл

**Шлях:** `app/src/main/java/com/worldmates/messenger/ui/groups/TopicsComponents.kt`

**Клік правою кнопкою на папку `ui/groups`** → New → Kotlin Class/File → **TopicsComponents**

---

### 6.2 Вставити код

**Весь код файлу:**

```kotlin
package com.worldmates.messenger.ui.groups

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldmates.messenger.data.model.Topic

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
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("Назва топіка") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text("Іконка:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(iconOptions) { icon ->
                        Surface(
                            shape = CircleShape,
                            color = if (icon == selectedIcon) Color(0xFFE3F2FD) else Color.Transparent,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedIcon = icon }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(icon, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Колір:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                onClick = {
                    onCreate(topicName, selectedIcon, selectedColor)
                    onDismiss()
                },
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

**Збережіть файл!**

---

## Крок 7: Інтеграція в MessagesActivity

### 7.1 Відкрити файл

**Шлях:** `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesActivity.kt`

---

### 7.2 Додати import

Знайдіть блок imports вгорі файлу і додайте:

```kotlin
import com.worldmates.messenger.ui.groups.TopicsTabRow
import com.worldmates.messenger.ui.groups.CreateTopicDialog
import com.worldmates.messenger.data.model.Topic
```

---

### 7.3 Додати state для топіків

Знайдіть в `MessagesScreenContent` де оголошені state змінні:

```kotlin
fun MessagesScreenContent(
    // ... параметри ...
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    // ... інші states ...
```

**Після останнього state** додайте:

```kotlin
    var showCreateTopicDialog by remember { mutableStateOf(false) }
```

---

### 7.4 Додати топіки над повідомленнями

Знайдіть в Column де рендериться список повідомлень:

```kotlin
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        MessagesTopBar(...)

        // Error message
        if (error != null) { ... }

        // Messages list  <--- ПЕРЕД ЦИМ БЛОКОМ додати топіки
        val listState = rememberLazyListState()
```

**ПЕРЕД `// Messages list`** додайте:

```kotlin
        // Topics (якщо це група)
        if (isGroup) {
            val groupsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.worldmates.messenger.ui.groups.GroupsViewModel>()
            val topics by groupsViewModel.topics.collectAsState()
            val selectedTopic by groupsViewModel.selectedTopic.collectAsState()

            // Завантажити топіки при відкритті
            LaunchedEffect(groupId) {
                groupsViewModel.loadTopics(groupId)
            }

            TopicsTabRow(
                topics = topics,
                selectedTopicId = selectedTopic?.id,
                onTopicSelect = { topicId ->
                    groupsViewModel.selectTopic(topicId)
                    // TODO: завантажити повідомлення топіка
                },
                onCreateTopic = { showCreateTopicDialog = true }
            )

            // Діалог створення топіка
            if (showCreateTopicDialog) {
                CreateTopicDialog(
                    onDismiss = { showCreateTopicDialog = false },
                    onCreate = { name, icon, color ->
                        groupsViewModel.createTopic(groupId, name, icon, color)
                        showCreateTopicDialog = false
                    }
                )
            }
        }
```

**Збережіть файл!**

---

## ✅ Перевірка роботи

### 1. Компіляція

```bash
./gradlew assembleDebug
```

Має скомпілюватися без помилок.

---

### 2. Тестування

1. **Відкрити групу** з вкладки "Групи"
2. **Побачити горизонтальні таби** вгорі екрану повідомлень
3. **Натиснути "+"** → діалог створення топіка
4. **Заповнити назву, вибрати іконку та колір**
5. **Натиснути "Створити"**
6. **Побачити новий топік** в табах

---

## 🐛 Типові помилки

### Помилка 1: "Unresolved reference: Topic"

**Рішення:** Додайте import:
```kotlin
import com.worldmates.messenger.data.model.Topic
```

### Помилка 2: "Cannot access database"

**Рішення:** Перевірте що таблиця Wo_GroupTopics створена:
```sql
SHOW TABLES LIKE 'Wo_GroupTopics';
```

### Помилка 3: "API returns 404"

**Рішення:** Перевірте що код додано в group_chat_v2.php і файл завантажено на сервер.

### Помилка 4: "Only admins can create topics"

**Рішення:** Це нормально! Тільки адміни груп можуть створювати топіки. Створіть свою групу і спробуйте знову.

---

## 🎯 Наступні кроки

Після реалізації топіків:

1. **Додати фільтрацію повідомлень по топіку**
2. **Показувати назву топіка в повідомленнях**
3. **Додати можливість редагувати/видаляти топіки**
4. **Створювати General топік автоматично при створенні групи**

---

## 📞 Допомога

Якщо щось не працює:

1. Перевірте логи сервера: `/var/www/.../api/v2/logs/group_chat_v2.log`
2. Перевірте Android logcat: `adb logcat | grep GroupsViewModel`
3. Перевірте API через Postman/curl
4. Перечитайте інструкцію покроково

**Всі файли з кодом знаходяться в:** `GROUPS_FEATURES_ROADMAP.md`

---

**Успіхів! 🚀**
