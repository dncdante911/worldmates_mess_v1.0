package com.worldmates.messenger.ui.chats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Chat
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SocketManager
import com.worldmates.messenger.utils.DecryptionUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatsViewModel : ViewModel(), SocketManager.SocketListener {

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var socketManager: SocketManager? = null

    init {
        fetchChats()
        setupSocket()
    }

    /**
     * Завантажує список чатів з сервера
     */
    fun fetchChats() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Виклик API для отримання чатів
                val response = RetrofitClient.apiService.getChats(
                    accessToken = UserSession.accessToken!!,
                    limit = 50,
                    dataType = "all",
                    setOnline = 1
                )

                if (response.apiStatus == 200 && response.chats != null) {
                    // Дешифруємо останнє повідомлення у кожному чаті
                    val decryptedChats = response.chats!!.map { chat ->
                        val lastMessage = chat.lastMessage?.let { msg ->
                            val decryptedText = DecryptionUtility.decryptMessage(
                                msg.encryptedText,
                                msg.timeStamp
                            )
                            msg.copy(decryptedText = decryptedText)
                        }
                        chat.copy(lastMessage = lastMessage)
                    }

                    _chatList.value = decryptedChats
                    _error.value = null
                    Log.d("ChatsViewModel", "Завантажено ${decryptedChats.size} чатів")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження чатів"
                    Log.e("ChatsViewModel", "Помилка API: ${response.apiStatus}")
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("ChatsViewModel", "Помилка завантаження чатів", e)
            }
        }
    }

    /**
     * Налаштовує Socket.IO для отримання чатів у реальному часі
     */
    private fun setupSocket() {
        try {
            socketManager = SocketManager(this)
            socketManager?.connect()
            Log.d("ChatsViewModel", "Socket.IO налаштований")
        } catch (e: Exception) {
            Log.e("ChatsViewModel", "Помилка налаштування Socket.IO", e)
        }
    }

    /**
     * Callback для нових повідомлень
     */
    override fun onNewMessage(messageJson: JSONObject) {
        Log.d("ChatsViewModel", "Нове повідомлення отримано")
        // Оновлюємо список чатів при отриманні нового повідомлення
        fetchChats()
    }

    override fun onSocketConnected() {
        Log.i("ChatsViewModel", "Socket підключено успішно!")
        _error.value = null
    }

    override fun onSocketDisconnected() {
        Log.w("ChatsViewModel", "Socket відключено")
        _error.value = "Втрачено з'єднання"
    }

    override fun onSocketError(error: String) {
        Log.e("ChatsViewModel", "Помилка Socket: $error")
        _error.value = error
    }

    /**
     * Видаляє помилку при закритті
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        socketManager?.disconnect()
        Log.d("ChatsViewModel", "ViewModel очищена")
    }
}