package com.worldmates.messenger.ui.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.network.MediaUploader
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SocketManager
import com.worldmates.messenger.utils.DecryptionUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class MessagesViewModel(application: Application) :
    AndroidViewModel(application), SocketManager.ExtendedSocketListener {

    private val context = application

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

    private var recipientId: Long = 0
    private var groupId: Long = 0
    private var socketManager: SocketManager? = null
    private var mediaUploader: MediaUploader? = null
    private var fileManager: FileManager? = null

    fun initialize(recipientId: Long) {
        this.recipientId = recipientId
        this.groupId = 0
        fetchMessages()
        setupSocket()
        Log.d("MessagesViewModel", "Ініціалізація для користувача $recipientId")
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
    fun sendMessage(text: String) {
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
                        text = text
                    )
                } else {
                    RetrofitClient.apiService.sendMessage(
                        accessToken = UserSession.accessToken!!,
                        recipientId = recipientId,
                        text = text,
                        messageHashId = messageHashId
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
        try {
            socketManager = SocketManager(this)
            socketManager?.connect()
            Log.d("MessagesViewModel", "Socket.IO налаштований")
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Помилка Socket.IO", e)
        }
    }

    override fun onNewMessage(messageJson: JSONObject) {
        try {
            val timestamp = messageJson.getLong("time")
            val encryptedText = messageJson.getString("text")
            val mediaUrl = messageJson.optString("media", null)

            // Дешифруем текст
            val decryptedText = DecryptionUtility.decryptMessageOrOriginal(encryptedText, timestamp)

            // Дешифруем URL медиа
            val decryptedMediaUrl = DecryptionUtility.decryptMediaUrl(mediaUrl, timestamp)

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
            Log.d("MessagesViewModel", "Користувач $userId ${if (isTyping) "набирає" else "зупинив набір"}")
        }
    }

    override fun onUserOnline(userId: Long) {
        if (userId == recipientId) {
            _recipientOnlineStatus.value = true
            Log.d("MessagesViewModel", "Користувач $userId з'явився онлайн")
        }
    }

    override fun onUserOffline(userId: Long) {
        if (userId == recipientId) {
            _recipientOnlineStatus.value = false
            Log.d("MessagesViewModel", "Користувач $userId з'явився офлайн")
        }
    }

    /**
     * Отправляет событие "набирает текст" через Socket.IO
     */
    fun sendTypingStatus(isTyping: Boolean) {
        socketManager?.emit(Constants.SOCKET_EVENT_TYPING, JSONObject().apply {
            put("user_id", UserSession.userId)  // Кто печатает
            put("recipient_id", recipientId)  // Кому отправляем
        })
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Полностью дешифрует сообщение: текст и URL медиа.
     * Также пытается извлечь URL медиа из текста сообщения.
     */
    private fun decryptMessageFully(msg: Message): Message {
        // Дешифруем текст
        val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
            msg.encryptedText,
            msg.timeStamp
        )

        // Дешифруем URL медиа
        val decryptedMediaUrl = DecryptionUtility.decryptMediaUrl(
            msg.mediaUrl,
            msg.timeStamp
        )

        // Пытаемся извлечь URL медиа из текста, если mediaUrl пуст
        val finalMediaUrl = decryptedMediaUrl
            ?: DecryptionUtility.extractMediaUrlFromText(decryptedText)

        return msg.copy(
            decryptedText = decryptedText,
            decryptedMediaUrl = finalMediaUrl
        )
    }

    override fun onCleared() {
        super.onCleared()
        socketManager?.disconnect()
        Log.d("MessagesViewModel", "ViewModel очищена")
    }
}