/**
 * 📸 Приклад інтеграції Socket.IO для Stories
 *
 * Додайте цей код в StoryViewModel для real-time оновлень
 */

// 1. В StoryViewModel додайте:

import com.worldmates.messenger.network.SocketManager
import org.json.JSONObject

class StoryViewModel(
    private val context: Context  // Додайте context параметр
) : ViewModel(), SocketManager.SocketListener {

    private var socketManager: SocketManager? = null
    private val subscribedFriends = mutableSetOf<Long>()

    init {
        setupSocketIO()
    }

    /**
     * Налаштування Socket.IO для real-time stories
     */
    private fun setupSocketIO() {
        socketManager = SocketManager(this, context)
        socketManager?.connect()

        // Слухаємо події stories
        socketManager?.onStoryCreated { data ->
            handleNewStory(data)
        }

        socketManager?.onStoryDeleted { data ->
            handleStoryDeleted(data)
        }

        socketManager?.onStoryCommentAdded { data ->
            handleNewComment(data)
        }
    }

    /**
     * Підписатися на stories друзів
     */
    fun subscribeToFriendStories(friendIds: List<Long>) {
        if (friendIds.isEmpty()) return

        socketManager?.subscribeToStories(friendIds)
        subscribedFriends.addAll(friendIds)

        // Завантажуємо початкові stories через REST API (один раз)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.storiesApiService.getStories(
                    accessToken = UserSession.accessToken!!
                )

                if (response.apiStatus == 200 && response.stories != null) {
                    _stories.value = response.stories!!
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stories", e)
            }
        }
    }

    /**
     * Відписатися від stories
     */
    fun unsubscribeFromStories() {
        if (subscribedFriends.isNotEmpty()) {
            socketManager?.unsubscribeFromStories(subscribedFriends.toList())
            subscribedFriends.clear()
        }
    }

    /**
     * Обробка нової story (real-time)
     */
    private fun handleNewStory(data: JSONObject) {
        try {
            val userId = data.getLong("userId")
            val storyJson = data.getJSONObject("story")

            // Парсимо мінімізовану story
            val newStory = Story(
                id = storyJson.getLong("id"),
                userId = storyJson.getLong("uid"),
                username = storyJson.optString("un", ""),
                userAvatar = storyJson.optString("uav", ""),
                mediaUrl = storyJson.optString("med", ""),
                thumbnailUrl = storyJson.optString("thumb", ""),
                mediaType = storyJson.optString("type", "image"),
                duration = storyJson.optInt("dur", 0),
                createdTime = storyJson.getLong("ct"),
                expireTime = storyJson.getLong("exp"),
                viewsCount = storyJson.optInt("views", 0),
                commentsCount = storyJson.optInt("coms", 0),
                seen = false
            )

            // Додаємо нову story (або оновлюємо існуючу групу)
            val currentStories = _stories.value.toMutableList()

            // Перевіряємо чи є вже stories від цього користувача
            val userStoryIndex = currentStories.indexOfFirst { it.userId == userId }

            if (userStoryIndex != -1) {
                // Додаємо до існуючої групи stories
                val existingUserStory = currentStories[userStoryIndex]
                val updatedMedia = (existingUserStory.mediaItems + newStory).toMutableList()
                currentStories[userStoryIndex] = existingUserStory.copy(mediaItems = updatedMedia)
            } else {
                // Створюємо нову групу stories для користувача
                currentStories.add(0, newStory)
            }

            _stories.value = currentStories

            Log.d(TAG, "✅ New story from user $userId added via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new story", e)
        }
    }

    /**
     * Обробка видалення story
     */
    private fun handleStoryDeleted(data: JSONObject) {
        try {
            val storyId = data.getLong("storyId")

            // Видаляємо story зі списку
            _stories.value = _stories.value.mapNotNull { userStory ->
                val filteredMedia = userStory.mediaItems.filter { it.id != storyId }

                if (filteredMedia.isEmpty()) {
                    null // Видаляємо всю групу якщо не залишилось stories
                } else {
                    userStory.copy(mediaItems = filteredMedia)
                }
            }

            Log.d(TAG, "✅ Story $storyId deleted via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling story delete", e)
        }
    }

    /**
     * Обробка нового коментаря до story
     */
    private fun handleNewComment(data: JSONObject) {
        try {
            val storyId = data.getLong("storyId")
            val commentJson = data.getJSONObject("comment")

            // Збільшуємо лічильник коментарів
            _stories.value = _stories.value.map { userStory ->
                val updatedMedia = userStory.mediaItems.map { story ->
                    if (story.id == storyId) {
                        story.copy(commentsCount = story.commentsCount + 1)
                    } else {
                        story
                    }
                }
                userStory.copy(mediaItems = updatedMedia)
            }

            Log.d(TAG, "✅ New comment on story $storyId via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new comment", e)
        }
    }

    /**
     * Відправити перегляд story
     */
    fun viewStory(storyId: Long, storyOwnerId: Long) {
        socketManager?.sendStoryView(storyId, storyOwnerId)

        // Позначаємо story як переглянуту локально
        _stories.value = _stories.value.map { userStory ->
            if (userStory.userId == storyOwnerId) {
                val updatedMedia = userStory.mediaItems.map { story ->
                    if (story.id == storyId) {
                        story.copy(seen = true)
                    } else {
                        story
                    }
                }
                userStory.copy(mediaItems = updatedMedia, seen = true)
            } else {
                userStory
            }
        }
    }

    /**
     * Відправити typing в коментарях
     */
    fun sendTypingInStory(storyId: Long, storyOwnerId: Long, isTyping: Boolean) {
        socketManager?.sendStoryTyping(storyId, storyOwnerId, isTyping)
    }

    // ==================== SocketListener implementation ====================

    override fun onNewMessage(messageJson: JSONObject) {
        // Не використовується для stories
    }

    override fun onSocketConnected() {
        Log.d(TAG, "✅ Socket.IO connected for stories")
    }

    override fun onSocketDisconnected() {
        Log.d(TAG, "❌ Socket.IO disconnected for stories")
    }

    override fun onSocketError(error: String) {
        Log.e(TAG, "❌ Socket.IO error: $error")
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromStories()
        socketManager?.disconnect()
    }
}

// ========================================
// 2. В Activity/Composable:
// ========================================

@Composable
fun ChatsScreenModern(
    viewModel: StoryViewModel
) {
    // Отримуємо список друзів (можна з ChatsViewModel)
    val friends = remember { /* список ID друзів */ }

    // Підписуємось на stories при запуску
    LaunchedEffect(Unit) {
        viewModel.subscribeToFriendStories(friends)
    }

    // Відписуємось при закритті
    DisposableEffect(Unit) {
        onDispose {
            viewModel.unsubscribeFromStories()
        }
    }

    // Stories UI
    val stories by viewModel.stories.collectAsState()

    PersonalStoriesRow(
        stories = stories,
        onStoryClick = { story ->
            // Відкриваємо StoryViewerActivity
            // і відправляємо view
            viewModel.viewStory(story.id, story.userId)
        },
        onCreateStory = {
            // Створити нову story
        }
    )
}

// ========================================
// 3. В StoryViewerActivity:
// ========================================

class StoryViewerActivity : ComponentActivity() {

    private lateinit var viewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storyId = intent.getLongExtra("story_id", 0L)
        val storyOwnerId = intent.getLongExtra("story_owner_id", 0L)

        // Відправляємо view автоматично при відкритті
        viewModel.viewStory(storyId, storyOwnerId)

        // Решта коду...
    }
}

// ========================================
// 4. Видалити старий polling (якщо є):
// ========================================

// ❌ ВИДАЛИТИ:
/*
LaunchedEffect(Unit) {
    while (true) {
        delay(20000) // ❌ Polling
        viewModel.refreshStories()
    }
}
*/

// ✅ ЗАЛИШИТИ:
LaunchedEffect(Unit) {
    viewModel.subscribeToFriendStories(friendIds) // ✅ Один раз
}

// ========================================
// 5. Результат:
// ========================================

/*
 * До міграції:
 * - REST API polling кожні 20 сек
 * - Затримка 0-20 секунд
 * - Багато зайвих запитів
 *
 * Після міграції:
 * - WebSocket real-time
 * - Затримка 0 секунд ⚡
 * - Миттєві сповіщення про нові stories
 * - Автоматичне оновлення переглядів
 */
