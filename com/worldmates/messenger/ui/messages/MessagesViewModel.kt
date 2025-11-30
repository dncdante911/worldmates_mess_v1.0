package com.worldmates.messenger.ui.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SocketManager
import com.worldmates.messenger.utils.DecryptionUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class MessagesViewModel : ViewModel(), SocketManager.SocketListener {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var recipientId: Long = 0
    private var socketManager: SocketManager? = null

    fun initialize(recipientId: Long) {
        this.recipientId = recipientId
        fetchMessages()
        setupSocket()
        Log.d("MessagesViewModel", "Ініціалізація для користувача $recipientId")
    }

    /**
     * Завантажує історію повідомлень
     */
    fun fetchMessages(beforeMessageId: Long = 0) {
        if (UserSession.accessToken == null || recipientId == 0) {
            _error.value = "Помилка: не авторизовано"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(
                    accessToken = UserSession.accessToken!!,
                    recipientId = recipientId,
                    limit = 30,
                    beforeMessageId = beforeMessageId
                )

                if (response.apiStatus == 200 && response.messages != null) {
                    // Дешифруємо усі повідомлення
                    val decryptedMessages = response.messages!!.map { msg ->
                        val decryptedText = try {
                            DecryptionUtility.decryptMessage(msg.encryptedText, msg.timeStamp)
                                ?: "(Помилка дешифрування)"
                        } catch (e: Exception) {
                            Log.e("MessagesViewModel", "Помилка дешифрування", e)
                            "(Помилка дешифрування)"
                        }
                        msg.copy(decryptedText = decryptedText)
                    }

                    _messages.value = (_messages.value + decryptedMessages).distinctBy { it.id }
                    _error.value = null
                    Log.d("MessagesViewModel", "Завантажено ${decryptedMessages.size} повідомлень")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження повідомлень"
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
     * Надсилає повідомлення
     */
    fun sendMessage(text: String) {
        if (UserSession.accessToken == null || recipientId == 0 || text.isBlank()) {
            _error.value = "Не можна надіслати порожнє повідомлення"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Використовуємо REST API для надсилання
                val sendService = RetrofitClient.retrofit.create(SendMessageService::class.java)
                
                val response = sendService.sendMessage(
                    accessToken = UserSession.accessToken!!,
                    recipientId = recipientId,
                    text = text,
                    sendTime = System.currentTimeMillis() / 1000
                )

                if (response.contains("\"api_status\":\"200\"")) {
                    // Оновлюємо список повідомлень
                    fetchMessages()
                    _error.value = null
                    Log.d("MessagesViewModel", "Повідомлення надіслано")
                } else {
                    _error.value = "Не вдалося надіслати повідомлення"
                    Log.e("MessagesViewModel", "Помилка відповіді: $response")
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
     * Налаштовує Socket.IO
     */
    private fun setupSocket() {
        try {
            socketManager = SocketManager(this)
            socketManager?.connect()
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
                encryptedText = messageJson.getString("text"),
                timeStamp = messageJson.getLong("time"),
                mediaUrl = messageJson.optString("media", ""),
                type = messageJson.optString("type", "text"),
                decryptedText = DecryptionUtility.decryptMessage(
                    messageJson.getString("text"),
                    messageJson.getLong("time")
                ) ?: "(Помилка)"
            )

            if (message.fromId == recipientId || message.toId == recipientId) {
                val currentMessages = _messages.value.toMutableList()
                currentMessages.add(message)
                _messages.value = currentMessages
                Log.d("MessagesViewModel", "Нове повідомлення додано")
            }
        } catch (e: Exception) {
            Log.e("MessagesViewModel", "Помилка обробки повідомлення", e)
        }
    }

    override fun onSocketConnected() {
        Log.i("MessagesViewModel", "Socket підключено")
    }

    override fun onSocketDisconnected() {
        Log.w("MessagesViewModel", "Socket відключено")
    }

    override fun onSocketError(error: String) {
        Log.e("MessagesViewModel", "Помилка Socket: $error")
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        socketManager?.disconnect()
    }
}

@FormUrlEncoded
interface SendMessageService {
    @POST("?type=send_message")
    suspend fun sendMessage(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("text") text: String,
        @Field("send_time") sendTime: Long
    ): String
}