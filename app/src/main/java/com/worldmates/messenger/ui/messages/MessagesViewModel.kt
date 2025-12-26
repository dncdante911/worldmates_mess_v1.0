package com.worldmates.messenger.ui.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.data.model.MessageReaction
import com.worldmates.messenger.data.model.ReactionGroup
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.network.MediaUploader
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SocketManager
import com.worldmates.messenger.utils.DecryptionUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.worldmates.messenger.ui.messages.selection.ForwardRecipient
import org.json.JSONObject
import java.io.File

class MessagesViewModel(application: Application) :
    AndroidViewModel(application), SocketManager.ExtendedSocketListener {

    private val context = application

    init {
        Log.d("MessagesViewModel", "🚀 MessagesViewModel створено!")
        Log.d("MessagesViewModel", "Access Token: ${UserSession.accessToken?.take(10)}...")
        Log.d("MessagesViewModel", "User ID: ${UserSession.userId}")
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private val _recipientOnlineStatus = MutableStateFlow(false)
    val recipientOnlineStatus: StateFlow<Boolean> = _recipientOnlineStatus

    private val _forwardContacts = MutableStateFlow<List<ForwardRecipient>>(emptyList())
    val forwardContacts: StateFlow<List<ForwardRecipient>> = _forwardContacts

    private val _forwardGroups = MutableStateFlow<List<ForwardRecipient>>(emptyList())
    val forwardGroups: StateFlow<List<ForwardRecipient>> = _forwardGroups

    private var recipientId: Long = 0
    private var groupId: Long = 0
    private var socketManager: SocketManager? = null
    private var mediaUploader: MediaUploader? = null
    private var fileManager: FileManager? = null

    fun initialize(recipientId: Long) {
        Log.d("MessagesViewModel", "🔧 initialize() викликано для користувача $recipientId")
        this.recipientId = recipientId
        this.groupId = 0
        fetchMessages()
        setupSocket()
        Log.d("MessagesViewModel", "✅ Ініціалізація завершена для користувача $recipientId")
    }

    fun initializeGroup(groupId: Long) {
        this.groupId = groupId
        this.recipientId = 0
        fetchGroupMessages()
        setupSocket()
        Log.d("MessagesViewModel", "Ініціалізація для групи $groupId")
    }

    /**
     * Завантажує історію повідомлень для особистого чату
     */
    fun fetchMessages(beforeMessageId: Long = 0) {
        if (UserSession.accessToken == null || recipientId == 0L) {
            _error.value = "Помилка: не авторизовано або невірний ID"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(
                    accessToken = UserSession.accessToken!!,
                    recipientId = recipientId,
                    limit = Constants.MESSAGES_PAGE_SIZE,
                    beforeMessageId = beforeMessageId
                )

                if (response.apiStatus == 200 && response.messages != null) {
                    val decryptedMessages = response.messages!!.map { msg ->
                        decryptMessageFully(msg)
                    }

                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.addAll(decryptedMessages)
                    // Сортируем по времени (старые сверху, новые внизу)
                    _messages.value = currentMessages.distinctBy { it.id }.sortedBy { it.timeStamp }

                    _error.value = null
                    Log.d("MessagesViewModel", "Завантажено ${decryptedMessages.size} повідомлень")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження повідомлень"
                    Log.e("MessagesViewModel", "API Error: ${response.apiStatus}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка завантаження повідомлень", e)
            }
        }
    }

    /**
     * Завантажує повідомлення групи
     */
    private fun fetchGroupMessages(beforeMessageId: Long = 0) {
        if (UserSession.accessToken == null || groupId == 0L) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Використовуємо НОВИЙ API для групових повідомлень
                val response = RetrofitClient.apiService.getGroupMessages(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    limit = Constants.MESSAGES_PAGE_SIZE,
                    beforeMessageId = beforeMessageId
                )

                if (response.apiStatus == 200 && response.messages != null) {
                    val decryptedMessages = response.messages!!.map { msg ->
                        decryptMessageFully(msg)
                    }

                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.addAll(decryptedMessages)
                    // Сортируем по времени (старые сверху, новые внизу)
                    _messages.value = currentMessages.distinctBy { it.id }.sortedBy { it.timeStamp }

                    _error.value = null
                    Log.d("MessagesViewModel", "Завантажено ${decryptedMessages.size} повідомлень групи")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження повідомлень"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка завантаження повідомлень групи", e)
            }
        }
    }

    /**
     * Надсилает текстовое сообщение
     */
    fun sendMessage(text: String, replyToId: Long? = null) {
        if (UserSession.accessToken == null || (recipientId == 0L && groupId == 0L) || text.isBlank()) {
            _error.value = "Не можна надіслати порожнє повідомлення"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val messageHashId = System.currentTimeMillis().toString()

                val response = if (groupId != 0L) {
                    // Використовуємо НОВИЙ API для відправки в групу
                    RetrofitClient.apiService.sendGroupMessage(
                        accessToken = UserSession.accessToken!!,
                        groupId = groupId,
                        text = text,
                        replyToId = replyToId
                    )
                } else {
                    RetrofitClient.apiService.sendMessage(
                        accessToken = UserSession.accessToken!!,
                        recipientId = recipientId,
                        text = text,
                        messageHashId = messageHashId,
                        replyToId = replyToId
                    )
                }

                Log.d("MessagesViewModel", "API Response: status=${response.apiStatus}, messages=${response.messages?.size}, errors=${response.errors}")

                if (response.apiStatus == 200) {
                    // Если API вернул сообщения, добавляем их в список
                    if (response.messages != null && response.messages.isNotEmpty()) {
                        val decryptedMessages = response.messages.map { msg ->
                            decryptMessageFully(msg)
                        }

                        val currentMessages = _messages.value.toMutableList()
                        currentMessages.addAll(decryptedMessages)
                        // Сортируем по времени (старые сверху, новые внизу)
                        _messages.value = currentMessages.distinctBy { it.id }.sortedBy { it.timeStamp }
                        Log.d("MessagesViewModel", "Додано ${decryptedMessages.size} нових повідомлень")
                    } else {
                        // Если API не вернул сообщения, перезагружаем весь список
                        Log.d("MessagesViewModel", "API не повернув повідомлення, перезавантажуємо список")
                        if (groupId != 0L) {
                            fetchGroupMessages()
                        } else {
                            fetchMessages()
                        }
                    }

                    // КРИТИЧНО: Эмитим Socket.IO событие для real-time доставки
                    if (groupId != 0L) {
                        socketManager?.sendGroupMessage(groupId, text)
                        Log.d("MessagesViewModel", "Socket.IO: Відправлено групове повідомлення")
                    } else {
                        socketManager?.sendMessage(recipientId, text)
                        Log.d("MessagesViewModel", "Socket.IO: Відправлено приватне повідомлення")
                    }

                    _error.value = null
                    Log.d("MessagesViewModel", "Повідомлення надіслано")
                } else {
                    _error.value = response.errors?.errorText ?: response.errorMessage ?: "Не вдалося надіслати повідомлення"
                    Log.e("MessagesViewModel", "Send Error: ${response.errors?.errorText ?: response.errorMessage}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка надсилання повідомлення", e)
            }
        }
    }

    /**
     * Редагує повідомлення
     */
    fun editMessage(messageId: Long, newText: String) {
        if (UserSession.accessToken == null || newText.isBlank()) {
            _error.value = "Не можна зберегти порожнє повідомлення"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.editMessage(
                    accessToken = UserSession.accessToken!!,
                    messageId = messageId,
                    newText = newText
                )

                if (response.apiStatus == 200) {
                    // Оновлюємо повідомлення в локальному списку
                    val currentMessages = _messages.value.toMutableList()
                    val index = currentMessages.indexOfFirst { it.id == messageId }

                    if (index != -1) {
                        val updatedMessage = currentMessages[index].copy(
                            encryptedText = newText,
                            decryptedText = newText
                        )
                        currentMessages[index] = updatedMessage
                        _messages.value = currentMessages
                        Log.d("MessagesViewModel", "Повідомлення відредаговано: $messageId")
                    }

                    _error.value = null
                } else {
                    _error.value = response.errors?.errorText ?: "Не вдалося відредагувати повідомлення"
                    Log.e("MessagesViewModel", "Edit Error: ${response.errors?.errorText}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка редагування повідомлення", e)
            }
        }
    }

    /**
     * Видаляє повідомлення
     */
    fun deleteMessage(messageId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteMessage(
                    accessToken = UserSession.accessToken!!,
                    messageId = messageId
                )

                if (response.apiStatus == 200) {
                    // Видаляємо повідомлення з локального списку
                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.removeAll { it.id == messageId }
                    _messages.value = currentMessages
                    Log.d("MessagesViewModel", "Повідомлення видалено: $messageId")

                    _error.value = null
                } else {
                    _error.value = response.errors?.errorText ?: "Не вдалося видалити повідомлення"
                    Log.e("MessagesViewModel", "Delete Error: ${response.errors?.errorText}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка видалення повідомлення", e)
            }
        }
    }

    // ==================== РЕАКЦІЇ ====================

    /**
     * Додає або видаляє реакцію на повідомлення (toggle)
     */
    fun toggleReaction(messageId: Long, emoji: String) {
        if (UserSession.accessToken == null) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        viewModelScope.launch {
            try {
                // Перевіряємо, чи вже є реакція від поточного користувача
                val message = _messages.value.find { it.id == messageId }
                val existingReactions = message?.reactions ?: emptyList()
                val hasMyReaction = existingReactions.any {
                    it.userId == UserSession.userId && it.reaction == emoji
                }

                val response = if (hasMyReaction) {
                    // Видаляємо реакцію
                    RetrofitClient.apiService.removeReaction(
                        accessToken = UserSession.accessToken!!,
                        messageId = messageId,
                        reaction = emoji
                    )
                } else {
                    // Додаємо реакцію
                    RetrofitClient.apiService.addReaction(
                        accessToken = UserSession.accessToken!!,
                        messageId = messageId,
                        reaction = emoji
                    )
                }

                if (response.apiStatus == 200) {
                    // Оновлюємо реакції для повідомлення
                    fetchReactionsForMessage(messageId)
                    Log.d("MessagesViewModel", "Реакцію ${if (hasMyReaction) "видалено" else "додано"}")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося оновити реакцію"
                    Log.e("MessagesViewModel", "Reaction Error: ${response.errorMessage}")
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                Log.e("MessagesViewModel", "Помилка оновлення реакції", e)
            }
        }
    }

    /**
     * Завантажує реакції для конкретного повідомлення
     */
    private suspend fun fetchReactionsForMessage(messageId: Long) {
        try {
            val response = RetrofitClient.apiService.getReactions(
                accessToken = UserSession.accessToken!!,
                messageId = messageId
            )

            if (response.apiStatus == 200 && response.reactions != null) {
                // Оновлюємо список повідомлень з новими реакціями
                val currentMessages = _messages.value.toMutableList()
                val messageIndex = currentMessages.indexOfFirst { it.id == messageId }

                if (messageIndex != -1) {
                    val updatedMessage = currentMessages[messageIndex].copy(
                        reactions = response.reactions
                    )
                    currentMessages[messageIndex] = updatedMessage
                    _messages.value = currentMessages
                }
            }
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Помилка завантаження реакцій", e)
        }
    }

    /**
     * Групує реакції по емоджі для відображення під повідомленням
     */
    fun getReactionGroups(reactions: List<MessageReaction>): List<ReactionGroup> {
        return reactions.groupBy { it.reaction }
            .map { (emoji, reactionList) ->
                ReactionGroup(
                    emoji = emoji,
                    count = reactionList.size,
                    userIds = reactionList.map { it.userId },
                    hasMyReaction = reactionList.any { it.userId == UserSession.userId }
                )
            }
    }

    // ==================== СТІКЕРИ ====================

    /**
     * Надсилає стікер
     */
    fun sendSticker(stickerId: Long) {
        if (UserSession.accessToken == null || (recipientId == 0L && groupId == 0L)) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val messageHashId = java.util.UUID.randomUUID().toString()

                val response = RetrofitClient.apiService.sendSticker(
                    accessToken = UserSession.accessToken!!,
                    recipientId = recipientId.takeIf { it != 0L },
                    groupId = groupId.takeIf { it != 0L },
                    stickerId = stickerId,
                    messageHashId = messageHashId
                )

                if (response.apiStatus == 200) {
                    // Перезавантажуємо повідомлення
                    if (groupId != 0L) {
                        fetchGroupMessages()
                    } else {
                        fetchMessages()
                    }

                    _error.value = null
                    Log.d("MessagesViewModel", "Стікер надіслано")
                } else {
                    _error.value = response.errors?.errorText ?: response.errorMessage ?: "Не вдалося надіслати стікер"
                    Log.e("MessagesViewModel", "Send Sticker Error: ${response.errors?.errorText ?: response.errorMessage}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("MessagesViewModel", "Помилка надсилання стікера", e)
            }
        }
    }

    /**
     * 🎬 Надсилає GIF
     */
    fun sendGif(gifUrl: String) {
        if (UserSession.accessToken == null || (recipientId == 0L && groupId == 0L)) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        if (gifUrl.isBlank()) {
            _error.value = "GIF URL не може бути порожнім"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val messageHashId = java.util.UUID.randomUUID().toString()

                // Відправляємо GIF як медіа-повідомлення
                val response = RetrofitClient.apiService.sendMessage(
                    accessToken = UserSession.accessToken!!,
                    toId = recipientId.takeIf { it != 0L },
                    groupId = groupId.takeIf { it != 0L },
                    message = "",  // Пусте текстове повідомлення
                    media = gifUrl,  // GIF URL як медіа
                    messageHashId = messageHashId,
                    replyId = null
                )

                if (response.apiStatus == 200) {
                    Log.d(TAG, "✅ GIF sent successfully: $gifUrl")

                    // Перезавантажуємо повідомлення
                    if (groupId != 0L) {
                        fetchGroupMessages()
                    } else {
                        fetchMessages()
                    }

                    _error.value = null
                    Log.d(TAG, "GIF надіслано")
                } else {
                    _error.value = response.errors?.errorText ?: response.errorMessage ?: "Не вдалося надіслати GIF"
                    Log.e(TAG, "Send GIF Error: ${response.errors?.errorText ?: response.errorMessage}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e(TAG, "Помилка надсилання GIF", e)
            }
        }
    }

    /**
     * Загружает и отправляет медиа-файл
     */
    fun uploadAndSendMedia(file: File, mediaType: String) {
        if (UserSession.accessToken == null || (recipientId == 0L && groupId == 0L)) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        if (!file.exists()) {
            _error.value = "Файл не знайдено"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                if (mediaUploader == null) {
                    mediaUploader = MediaUploader(context)
                }

                val result = mediaUploader!!.uploadMedia(
                    accessToken = UserSession.accessToken!!,
                    mediaType = mediaType,
                    filePath = file.absolutePath,
                    recipientId = recipientId.takeIf { it != 0L },
                    groupId = groupId.takeIf { it != 0L },
                    isPremium = false,
                    onProgress = { progress ->
                        _uploadProgress.value = progress
                    }
                )

                when (result) {
                    is MediaUploader.UploadResult.Success -> {
                        _uploadProgress.value = 0
                        _error.value = null
                        Log.d("MessagesViewModel", "Медіа завантажено: ${result.url}")

                        // Обновляем список сообщений для автообновления
                        if (groupId != 0L) {
                            fetchGroupMessages()
                        } else {
                            fetchMessages()
                        }

                        // Чистимо файл
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    is MediaUploader.UploadResult.Error -> {
                        _error.value = result.message
                        _uploadProgress.value = 0
                        Log.e("MessagesViewModel", "Помилка завантаження: ${result.message}")
                    }
                    is MediaUploader.UploadResult.Progress -> {
                        _uploadProgress.value = result.percent
                    }
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                _uploadProgress.value = 0
                Log.e("MessagesViewModel", "Помилка завантаження медіа", e)
            }
        }
    }

    /**
     * Отправляет сообщение с медиа-ссылкой
     */
    private fun sendMediaMessage(mediaUrl: String, mediaType: String, caption: String) {
        if (UserSession.accessToken == null) return

        // Для простоты отправляем как текстовое сообщение с URL
        val messageText = if (caption.isNotEmpty()) "$caption\n$mediaUrl" else "📎 $mediaType"
        sendMessage(messageText)
    }

    /**
     * Налаштовує Socket.IO для получения сообщений в реальном времени
     */
    private fun setupSocket() {
        Log.d("MessagesViewModel", "🔌 setupSocket() викликано")
        try {
            socketManager = SocketManager(this)
            Log.d("MessagesViewModel", "✅ SocketManager створено")
            socketManager?.connect()
            Log.d("MessagesViewModel", "✅ Socket.IO connect() викликано")
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "❌ Помилка Socket.IO", e)
            e.printStackTrace()
        }
    }

    override fun onNewMessage(messageJson: JSONObject) {
        try {
            Log.d("MessagesViewModel", "📨 Отримано Socket.IO повідомлення: $messageJson")

            val timestamp = messageJson.getLong("time")
            val encryptedText = messageJson.getString("text")
            val mediaUrl = messageJson.optString("media", null)

            // Поддержка AES-GCM (v2) - новые поля
            val iv = messageJson.optString("iv", null)?.takeIf { it.isNotEmpty() }
            val tag = messageJson.optString("tag", null)?.takeIf { it.isNotEmpty() }
            val cipherVersion = if (messageJson.has("cipher_version")) {
                messageJson.getInt("cipher_version")
            } else null

            // Дешифруем текст с поддержкой GCM
            val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
                text = encryptedText,
                timestamp = timestamp,
                iv = iv,
                tag = tag,
                cipherVersion = cipherVersion
            )

            // Дешифруем URL медиа с поддержкой GCM
            val decryptedMediaUrl = DecryptionUtility.decryptMediaUrl(
                mediaUrl = mediaUrl,
                timestamp = timestamp,
                iv = iv,
                tag = tag,
                cipherVersion = cipherVersion
            )

            // Пытаемся извлечь URL медиа из текста, если mediaUrl пуст
            val finalMediaUrl = decryptedMediaUrl
                ?: DecryptionUtility.extractMediaUrlFromText(decryptedText)

            val message = Message(
                id = messageJson.getLong("id"),
                fromId = messageJson.getLong("from_id"),
                toId = messageJson.getLong("to_id"),
                groupId = messageJson.optLong("group_id", 0).takeIf { it != 0L },
                encryptedText = encryptedText,
                timeStamp = timestamp,
                mediaUrl = mediaUrl,
                type = messageJson.optString("type", Constants.MESSAGE_TYPE_TEXT),
                senderName = messageJson.optString("sender_name", null),
                senderAvatar = messageJson.optString("sender_avatar", null),
                // Поля для AES-GCM (v2)
                iv = iv,
                tag = tag,
                cipherVersion = cipherVersion,
                // Дешифрованные данные
                decryptedText = decryptedText,
                decryptedMediaUrl = finalMediaUrl
            )

            // Проверяем, принадлежит ли сообщение текущему диалогу
            val isRelevant = if (groupId != 0L) {
                message.groupId == groupId
            } else {
                (message.fromId == recipientId && message.toId == UserSession.userId) ||
                (message.fromId == UserSession.userId && message.toId == recipientId)
            }

            if (isRelevant) {
                val currentMessages = _messages.value.toMutableList()
                currentMessages.add(message)
                // Сортируем по времени (старые сверху, новые внизу)
                _messages.value = currentMessages.distinctBy { it.id }.sortedBy { it.timeStamp }
                Log.d("MessagesViewModel", "Додано нове повідомлення від Socket.IO: ${message.decryptedText}")
                Log.d("MessagesViewModel", "Нове повідомлення додано")
            }
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Помилка обробки повідомлення", e)
        }
    }

    override fun onSocketConnected() {
        Log.i("MessagesViewModel", "Socket підключено успішно")
        _error.value = null
    }

    override fun onSocketDisconnected() {
        Log.w("MessagesViewModel", "Socket відключено")
        _error.value = "Втрачено з'єднання з сервером"
    }

    override fun onSocketError(error: String) {
        Log.e("MessagesViewModel", "Помилка Socket: $error")
        _error.value = error
    }

    override fun onTypingStatus(userId: Long, isTyping: Boolean) {
        if (userId == recipientId) {
            _isTyping.value = isTyping
            // ВАЖНО: Если пользователь печатает, значит он онлайн!
            if (isTyping) {
                _recipientOnlineStatus.value = true
            }
            Log.d("MessagesViewModel", "Користувач $userId ${if (isTyping) "набирає" else "зупинив набір"}")
        }
    }

    override fun onUserOnline(userId: Long) {
        if (userId == recipientId) {
            _recipientOnlineStatus.value = true
            Log.d("MessagesViewModel", "✅ Користувач $userId з'явився онлайн")
        }
    }

    override fun onUserOffline(userId: Long) {
        if (userId == recipientId) {
            // ВАЖНО: Не сбрасываем статус, если пользователь печатает
            if (!_isTyping.value) {
                _recipientOnlineStatus.value = false
                Log.d("MessagesViewModel", "❌ Користувач $userId з'явився офлайн")
            } else {
                Log.d("MessagesViewModel", "⚠️ Ігноруємо offline для $userId (друкує)")
            }
        }
    }

    /**
     * Отправляет событие "набирает текст" через Socket.IO
     */
    fun sendTypingStatus(isTyping: Boolean) {
        if (recipientId == 0L) return

        socketManager?.emit(Constants.SOCKET_EVENT_TYPING, JSONObject().apply {
            put("user_id", UserSession.userId)  // Кто печатает
            put("recipient_id", recipientId)  // Кому отправляем
            // Формат WoWonder: is_typing = 200 (печатает) или 300 (закончил)
            put("is_typing", if (isTyping) 200 else 300)
        })
        Log.d("MessagesViewModel", "Відправлено статус 'печатає': $isTyping для користувача $recipientId")
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Полностью дешифрует сообщение: текст и URL медиа.
     * Также пытается извлечь URL медиа из текста сообщения.
     * Поддерживает AES-GCM (v2) и обратную совместимость с AES-ECB (v1).
     */
    private fun decryptMessageFully(msg: Message): Message {
        // Дешифруем текст с поддержкой GCM
        val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
            text = msg.encryptedText,
            timestamp = msg.timeStamp,
            iv = msg.iv,
            tag = msg.tag,
            cipherVersion = msg.cipherVersion
        )

        // Дешифруем URL медиа с поддержкой GCM
        val decryptedMediaUrl = DecryptionUtility.decryptMediaUrl(
            mediaUrl = msg.mediaUrl,
            timestamp = msg.timeStamp,
            iv = msg.iv,
            tag = msg.tag,
            cipherVersion = msg.cipherVersion
        )

        // Пытаемся извлечь URL медиа из текста, если mediaUrl пуст
        val finalMediaUrl = decryptedMediaUrl
            ?: DecryptionUtility.extractMediaUrlFromText(decryptedText)

        return msg.copy(
            decryptedText = decryptedText,
            decryptedMediaUrl = finalMediaUrl
        )
    }

    /**
     * 📤 Завантажує список контактів для пересилання
     */
    fun loadForwardContacts() {
        if (UserSession.accessToken == null) {
            Log.e("MessagesViewModel", "Не авторизовано")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChats(
                    accessToken = UserSession.accessToken!!,
                    dataType = "users", // Тільки користувачі
                    limit = 100
                )

                if (response.apiStatus == 200) {
                    // Конвертуємо чати в ForwardRecipient
                    val contacts = response.chats?.map { chat ->
                        ForwardRecipient(
                            id = chat.userId,
                            name = chat.username ?: "Користувач",
                            avatarUrl = chat.avatarUrl ?: "",
                            isGroup = false
                        )
                    } ?: emptyList()

                    _forwardContacts.value = contacts
                    Log.d("MessagesViewModel", "Завантажено ${contacts.size} контактів для пересилання")
                } else {
                    Log.e("MessagesViewModel", "Помилка завантаження контактів: ${response.errorMessage}")
                }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Помилка завантаження контактів", e)
            }
        }
    }

    /**
     * 📤 Завантажує список груп для пересилання
     */
    fun loadForwardGroups() {
        if (UserSession.accessToken == null) {
            Log.e("MessagesViewModel", "Не авторизовано")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroups(
                    accessToken = UserSession.accessToken!!,
                    type = "get_list",
                    limit = 100
                )

                if (response.apiStatus == 200) {
                    // Конвертуємо групи в ForwardRecipient
                    val groups = response.groups?.map { group ->
                        ForwardRecipient(
                            id = group.id,
                            name = group.name,
                            avatarUrl = group.avatarUrl,
                            isGroup = true
                        )
                    } ?: emptyList()

                    _forwardGroups.value = groups
                    Log.d("MessagesViewModel", "Завантажено ${groups.size} груп для пересилання")
                } else {
                    Log.e("MessagesViewModel", "Помилка завантаження груп: ${response.errorMessage}")
                }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Помилка завантаження груп", e)
            }
        }
    }

    /**
     * 📤 Пересилає повідомлення до вибраних отримувачів
     */
    fun forwardMessages(messageIds: Set<Long>, recipientIds: List<Long>) {
        if (UserSession.accessToken == null) {
            Log.e("MessagesViewModel", "Не авторизовано")
            return
        }

        viewModelScope.launch {
            try {
                messageIds.forEach { messageId ->
                    // Знаходимо повідомлення
                    val message = _messages.value.find { it.id == messageId }
                    if (message != null) {
                        recipientIds.forEach { recipientId ->
                            // Визначаємо чи це група чи користувач
                            val isGroup = _forwardGroups.value.any { it.id == recipientId }

                            if (isGroup) {
                                // Пересилаємо в групу
                                RetrofitClient.apiService.sendGroupMessage(
                                    accessToken = UserSession.accessToken!!,
                                    type = "send_message",
                                    groupId = recipientId,
                                    text = message.decryptedText ?: ""
                                )
                                Log.d("MessagesViewModel", "Переслано повідомлення $messageId в групу $recipientId")
                            } else {
                                // Пересилаємо користувачу
                                val messageHashId = "${System.currentTimeMillis()}_${(0..999999).random()}"
                                RetrofitClient.apiService.sendMessage(
                                    accessToken = UserSession.accessToken!!,
                                    recipientId = recipientId,
                                    text = message.decryptedText ?: "",
                                    messageHashId = messageHashId
                                )
                                Log.d("MessagesViewModel", "Переслано повідомлення $messageId користувачу $recipientId")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MessagesViewModel", "Помилка пересилання", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager?.disconnect()
        Log.d("MessagesViewModel", "ViewModel очищена")
    }
}