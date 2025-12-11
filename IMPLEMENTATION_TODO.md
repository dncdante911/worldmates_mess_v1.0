# 🚀 Що треба зробити далі

## 📋 Пріоритет 1: Система топіків/тем (як Telegram)

### Що це?

Топіки дозволяють організувати обговорення в великих групах. Кожен топік - це окремий потік повідомлень з власною назвою та іконкою.

**Приклад використання:**
```
Група "Розробка проекту"
├── 💬 Загальне (General)
├── 🐛 Баги
├── 💡 Нові ідеї
├── 📢 Оголошення
└── 🎨 Дизайн
```

### Переваги:
- ✅ Організація обговорень за темами
- ✅ Легше знайти потрібну інформацію
- ✅ Зменшення шуму в великих групах
- ✅ Можливість підписатися на конкретні топіки

---

### Кроки реалізації:

#### 1️⃣ База даних

**Створити таблицю Wo_GroupTopics:**

```sql
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

**Додати колонку в Wo_Messages (якщо ще не додана):**

```sql
-- Спочатку перевірте:
SHOW COLUMNS FROM Wo_Messages LIKE 'topic_id';

-- Якщо колонки немає:
ALTER TABLE Wo_Messages ADD COLUMN topic_id INT UNSIGNED DEFAULT NULL;
ALTER TABLE Wo_Messages ADD INDEX idx_topic_id (topic_id);
```

---

#### 2️⃣ Server API

**Додати в group_chat_v2.php нові endpoints:**

Відкрити файл: `server_modifications/group_chat_v2.php`

Знайти секцію `switch ($type)` і додати:

```php
case 'create_topic':
    // [КОД З ROADMAP - РОЗДІЛ "Створити топік"]
    break;

case 'get_topics':
    // [КОД З ROADMAP - РОЗДІЛ "Отримати топіки групи"]
    break;

case 'get_topic_messages':
    // [КОД З ROADMAP - РОЗДІЛ "Отримати повідомлення топіка"]
    break;

case 'send_topic_message':
    // [КОД З ROADMAP - РОЗДІЛ "Надіслати повідомлення в топік"]
    break;

case 'update_topic':
    // Оновити назву/іконку топіка
    break;

case 'delete_topic':
    // Видалити топік (тільки не General)
    break;
```

**Повний код знайдете в:** `GROUPS_FEATURES_ROADMAP.md` → розділ "Server API Endpoints"

---

#### 3️⃣ Android Models

**Додати в:** `app/src/main/java/com/worldmates/messenger/data/model/Group.kt`

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

---

#### 4️⃣ Android API

**Додати в:** `app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt`

```kotlin
// Отримати топіки групи
@POST("/api/v2/group_chat_v2.php")
@FormUrlEncoded
suspend fun getTopics(
    @Query("access_token") accessToken: String,
    @Query("type") type: String = "get_topics",
    @Field("group_id") groupId: Long
): TopicListResponse

// Створити топік
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

// Отримати повідомлення топіка
@POST("/api/v2/group_chat_v2.php")
@FormUrlEncoded
suspend fun getTopicMessages(
    @Query("access_token") accessToken: String,
    @Query("type") type: String = "get_topic_messages",
    @Field("topic_id") topicId: Long,
    @Field("limit") limit: Int = 50
): MessageListResponse

// Надіслати повідомлення в топік
@POST("/api/v2/group_chat_v2.php")
@FormUrlEncoded
suspend fun sendTopicMessage(
    @Query("access_token") accessToken: String,
    @Query("type") type: String = "send_topic_message",
    @Field("topic_id") topicId: Long,
    @Field("text") text: String
): MessageListResponse
```

---

#### 5️⃣ ViewModel

**Додати в:** `app/src/main/java/com/worldmates/messenger/ui/groups/GroupsViewModel.kt`

```kotlin
private val _topics = MutableStateFlow<List<Topic>>(emptyList())
val topics: StateFlow<List<Topic>> = _topics

private val _selectedTopic = MutableStateFlow<Topic?>(null)
val selectedTopic: StateFlow<Topic?> = _selectedTopic

fun loadTopics(groupId: Long) {
    viewModelScope.launch {
        try {
            val response = RetrofitClient.apiService.getTopics(
                accessToken = UserSession.accessToken!!,
                groupId = groupId
            )
            if (response.apiStatus == 200 && response.topics != null) {
                _topics.value = response.topics
            }
        } catch (e: Exception) {
            _error.value = "Помилка завантаження топіків: ${e.message}"
        }
    }
}

fun createTopic(groupId: Long, name: String, icon: String, color: String) {
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
                loadTopics(groupId) // Оновити список
            }
        } catch (e: Exception) {
            _error.value = "Помилка створення топіка: ${e.message}"
        }
    }
}
```

---

#### 6️⃣ UI Components

**Створити новий файл:** `app/src/main/java/com/worldmates/messenger/ui/groups/TopicsComponents.kt`

Скопіювати код з `GROUPS_FEATURES_ROADMAP.md`:
- `TopicsTabRow` - горизонтальні табі з топіками
- `CreateTopicDialog` - діалог створення топіка

---

#### 7️⃣ Інтеграція в MessagesActivity

**Модифікувати:** `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesActivity.kt`

Додати над списком повідомлень:

```kotlin
// Якщо це група - показати топіки
if (isGroup) {
    val topics by viewModel.topics.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()

    TopicsTabRow(
        topics = topics,
        selectedTopicId = selectedTopic?.id,
        onTopicSelect = { topicId ->
            viewModel.selectTopic(topicId)
            viewModel.loadTopicMessages(topicId)
        },
        onCreateTopic = { showCreateTopicDialog = true }
    )
}
```

---

## 📋 Пріоритет 2: Покращення UX

### Що треба додати:

#### 1. Пошук в повідомленнях групи
- Кнопка пошуку в GroupDetailsActivity
- Фільтрація повідомлень по тексту
- Виділення знайдених результатів

#### 2. Сповіщення груп
- Налаштування сповіщень для кожної групи
- Вимкнути/Увімкнути звуки
- Push notifications для нових повідомлень

#### 3. Закріплені повідомлення
- Можливість закріпити повідомлення вгорі
- Показ закріпленого повідомлення в хедері
- Тільки адміни можуть закріплювати

#### 4. Пересилання повідомлень
- Довге натискання → Переслати
- Вибір групи/чату для пересилання
- Збереження оригінального відправника

#### 5. Відповіді на повідомлення
- Свайп вправо → Відповісти
- Показ оригінального повідомлення
- Reply UI як в Telegram

---

## 📋 Пріоритет 3: Додаткові функції

### 1. Статистика груп
- Кількість повідомлень за день/тиждень
- Найактивніші учасники
- Графіки активності

### 2. Експорт чатів
- Експорт історії повідомлень у TXT/JSON
- Збереження медіа файлів
- Архівація груп

### 3. Боти в групах
- API для ботів
- Webhook endpoints
- Команди ботів (/start, /help)

### 4. Голосові/відео дзвінки в групах
- Інтеграція WebRTC
- Груповий відеочат до 8 осіб
- Демонстрація екрану

---

## 📖 Де знайти код?

**Всі готові приклади коду в:** `GROUPS_FEATURES_ROADMAP.md`

**Детальна інструкція як їх використати:** `IMPLEMENTATION_GUIDE.md`

---

## ⏱️ Оцінка часу

- **Топіки/теми:** 4-6 годин
- **Покращення UX:** 2-4 години
- **Додаткові функції:** 8-12 годин

**Всього:** ~14-22 години чистого кодування

---

## 🎯 Наступний крок

**Рекомендую почати з топіків**, бо:
1. Код майже готовий в roadmap
2. Це найбільш корисна функція
3. Покращить UX для великих груп

**Інструкція:** Дивіться `IMPLEMENTATION_GUIDE.md` → розділ "Топіки/Теми"
