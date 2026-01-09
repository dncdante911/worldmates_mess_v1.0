/**
 * 📢 Приклад інтеграції Socket.IO для каналів
 *
 * Додайте цей код в ChannelDetailsViewModel для real-time оновлень
 */

// 1. В ChannelDetailsViewModel додайте:

import com.worldmates.messenger.network.SocketManager
import org.json.JSONObject

class ChannelDetailsViewModel(
    private val context: Context  // Додайте context параметр
) : ViewModel(), SocketManager.SocketListener {

    private var socketManager: SocketManager? = null

    init {
        // Підключаємо Socket.IO
        setupSocketIO()
    }

    /**
     * Налаштування Socket.IO для real-time оновлень
     */
    private fun setupSocketIO() {
        socketManager = SocketManager(this, context)
        socketManager?.connect()

        // Слухаємо події каналів
        socketManager?.onChannelPostCreated { data ->
            handleNewPost(data)
        }

        socketManager?.onChannelPostUpdated { data ->
            handlePostUpdate(data)
        }

        socketManager?.onChannelPostDeleted { data ->
            handlePostDelete(data)
        }

        socketManager?.onChannelCommentAdded { data ->
            handleNewComment(data)
        }
    }

    /**
     * Підписатися на канал
     */
    fun subscribeToChannel(channelId: Long) {
        socketManager?.subscribeToChannel(channelId)

        // Завантажуємо історію постів через REST API (один раз)
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChannelPosts(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId,
                    limit = 20
                )

                if (response.apiStatus == 200 && response.posts != null) {
                    _posts.value = response.posts!!
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading posts", e)
            }
        }
    }

    /**
     * Відписатися від каналу
     */
    fun unsubscribeFromChannel(channelId: Long) {
        socketManager?.unsubscribeFromChannel(channelId)
    }

    /**
     * Обробка нового поста (real-time)
     */
    private fun handleNewPost(data: JSONObject) {
        try {
            val channelId = data.getLong("channelId")
            val postJson = data.getJSONObject("post")

            // Парсимо мінімізований пост
            val newPost = ChannelPost(
                id = postJson.getLong("id"),
                channelId = postJson.optLong("cid", channelId),
                userId = postJson.getLong("uid"),
                username = postJson.optString("un", ""),
                userName = postJson.optString("uname", ""),
                userAvatar = postJson.optString("uav", ""),
                text = postJson.optString("txt", ""),
                media = postJson.optString("med", null),
                createdTime = postJson.getLong("ct"),
                isPinned = postJson.optInt("pin", 0) == 1,
                viewsCount = postJson.optInt("views", 0),
                commentsCount = postJson.optInt("coms", 0),
                reactions = parseReactions(postJson.optJSONArray("reacts"))
            )

            // Додаємо на початок списку
            _posts.value = listOf(newPost) + _posts.value

            Log.d(TAG, "✅ New post added via Socket.IO: ${newPost.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new post", e)
        }
    }

    /**
     * Обробка оновлення поста
     */
    private fun handlePostUpdate(data: JSONObject) {
        try {
            val postId = data.getLong("postId")
            val newText = data.optString("text", null)
            val newMedia = data.optString("media", null)

            _posts.value = _posts.value.map { post ->
                if (post.id == postId) {
                    post.copy(
                        text = newText ?: post.text,
                        media = newMedia ?: post.media
                    )
                } else {
                    post
                }
            }

            Log.d(TAG, "✅ Post $postId updated via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling post update", e)
        }
    }

    /**
     * Обробка видалення поста
     */
    private fun handlePostDelete(data: JSONObject) {
        try {
            val postId = data.getLong("postId")

            _posts.value = _posts.value.filter { it.id != postId }

            Log.d(TAG, "✅ Post $postId deleted via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling post delete", e)
        }
    }

    /**
     * Обробка нового коментаря
     */
    private fun handleNewComment(data: JSONObject) {
        try {
            val postId = data.getLong("postId")
            val commentJson = data.getJSONObject("comment")

            // Збільшуємо лічильник коментарів
            _posts.value = _posts.value.map { post ->
                if (post.id == postId) {
                    post.copy(commentsCount = post.commentsCount + 1)
                } else {
                    post
                }
            }

            Log.d(TAG, "✅ New comment on post $postId via Socket.IO")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new comment", e)
        }
    }

    /**
     * Парсинг реакцій з JSON
     */
    private fun parseReactions(reactionsArray: JSONArray?): List<PostReaction> {
        if (reactionsArray == null) return emptyList()

        val reactions = mutableListOf<PostReaction>()
        for (i in 0 until reactionsArray.length()) {
            val reactionJson = reactionsArray.getJSONObject(i)
            reactions.add(
                PostReaction(
                    emoji = reactionJson.getString("emoji"),
                    count = reactionJson.getInt("count"),
                    userReacted = reactionJson.optBoolean("user_reacted", false),
                    recentUsers = emptyList() // TODO: parse if needed
                )
            )
        }
        return reactions
    }

    // ==================== SocketListener implementation ====================

    override fun onNewMessage(messageJson: JSONObject) {
        // Не використовується для каналів
    }

    override fun onSocketConnected() {
        Log.d(TAG, "✅ Socket.IO connected for channels")
        // Можна показати індикатор підключення
    }

    override fun onSocketDisconnected() {
        Log.d(TAG, "❌ Socket.IO disconnected for channels")
        // Можна показати індикатор відключення
    }

    override fun onSocketError(error: String) {
        Log.e(TAG, "❌ Socket.IO error: $error")
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        // Відключаємо Socket.IO
        currentChannelId?.let { unsubscribeFromChannel(it) }
        socketManager?.disconnect()
    }
}

// ========================================
// 2. В Activity/Composable:
// ========================================

@Composable
fun ChannelDetailsScreen(
    channelId: Long,
    viewModel: ChannelDetailsViewModel
) {
    // Підписуємось на канал при відкритті екрану
    LaunchedEffect(channelId) {
        viewModel.subscribeToChannel(channelId)
    }

    // Відписуємось при закритті
    DisposableEffect(channelId) {
        onDispose {
            viewModel.unsubscribeFromChannel(channelId)
        }
    }

    // Решта UI коду...
}

// ========================================
// 3. Видалити старий polling код (ВАЖЛИВО!):
// ========================================

// ❌ ВИДАЛИТИ ЦЕ:
/*
LaunchedEffect(Unit) {
    while (true) {
        delay(15000) // ❌ Polling кожні 15 секунд
        viewModel.refreshPosts()
    }
}
*/

// ✅ ЗАЛИШИТИ ТІЛЬКИ:
LaunchedEffect(channelId) {
    viewModel.subscribeToChannel(channelId) // ✅ Підписка один раз
}

// ========================================
// 4. Результат:
// ========================================

/*
 * До міграції:
 * - Polling кожні 15 секунд = 4 req/min
 * - Затримка 0-15 секунд
 * - Багато зайвого трафіку
 *
 * Після міграції:
 * - 1 WebSocket connection
 * - Затримка 0 секунд ⚡
 * - Тільки необхідні дані
 */
