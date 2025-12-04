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
    AndroidViewModel(application), SocketManager.SocketListener {

    private val context = application

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

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
                        val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
                            msg.encryptedText,
                            msg.timeStamp
                        )
                        msg.copy(decryptedText = decryptedText)
                    }

                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.addAll(decryptedMessages)
                    _messages.value = currentMessages.distinctBy { it.id }
                    
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
                val response = RetrofitClient.apiService.getMessages(
                    accessToken = UserSession.accessToken!!,
                    recipientId = groupId,
                    limit = Constants.MESSAGES_PAGE_SIZE,
                    beforeMessageId = beforeMessageId
                )

                if (response.apiStatus == 200 && response.messages != null) {
                    val decryptedMessages = response.messages!!.map { msg ->
                        val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
                            msg.encryptedText,
                            msg.timeStamp
                        )
                        msg.copy(decryptedText = decryptedText)
                    }

                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.addAll(decryptedMessages)
                    _messages.value = currentMessages.distinctBy { it.id }
                    
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
                val sendTime = System.currentTimeMillis() / 1000
                
                val response = if (groupId != 0L) {
                    RetrofitClient.apiService.sendGroupMessage(
                        accessToken = UserSession.accessToken!!,
                        groupId = groupId,
                        text = text,
                        sendTime = sendTime
                    )
                } else {
                    RetrofitClient.apiService.sendMessage(
                        accessToken = UserSession.accessToken!!,
                        userId = UserSession.userId,
                        recipientId = recipientId,
                        text = text,
                        sendTime = sendTime
                    )
                }

                if (response.apiStatus == 200) {
                    // Перезагружаем сообщения
                    if (groupId != 0L) {
                        fetchGroupMessages()
                    } else {
                        fetchMessages()
                    }
                    _error.value = null
                    Log.d("MessagesViewModel", "Повідомлення надіслано")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося надіслати повідомлення"
                    Log.e("MessagesViewModel", "Send Error: ${response.errorMessage}")
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
                        // Отправляем сообщение с медиа
                        sendMediaMessage(
                            mediaUrl = result.url,
                            mediaType = mediaType,
                            caption = ""
                        )
                        _uploadProgress.value = 0
                        _error.value = null
                        Log.d("MessagesViewModel", "Медіа завантажено: ${result.url}")
                        
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
            val message = Message(
                id = messageJson.getLong("id"),
                fromId = messageJson.getLong("from_id"),
                toId = messageJson.getLong("to_id"),
                groupId = messageJson.optLong("group_id", 0).takeIf { it != 0L },
                encryptedText = messageJson.getString("text"),
                timeStamp = messageJson.getLong("time"),
                mediaUrl = messageJson.optString("media", null),
                type = messageJson.optString("type", Constants.MESSAGE_TYPE_TEXT),
                senderName = messageJson.optString("sender_name", null),
                senderAvatar = messageJson.optString("sender_avatar", null),
                decryptedText = DecryptionUtility.decryptMessageOrOriginal(
                    messageJson.getString("text"),
                    messageJson.getLong("time")
                )
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
                _messages.value = currentMessages.distinctBy { it.id }
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

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        socketManager?.disconnect()
        Log.d("MessagesViewModel", "ViewModel очищена")
    }
}