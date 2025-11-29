package com.worldmates.messenger.ui.chats

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
import android.util.Log // Добавлен import для Log

class ChatsViewModel : ViewModel(), SocketManager.SocketListener {

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList

    private val socketManager = SocketManager(this)

    init {
        fetchChats()
        socketManager.connect()
    }

    /**
     * Загружает список чатов с сервера Worldmates, включая дешифрование последнего сообщения.
     * Соответствует логике в файле get_chats.php.
     */
    fun fetchChats() {
        if (UserSession.accessToken == null) return

        viewModelScope.launch {
            try {
                // Вызываем WorldMatesApi через RetrofitClient
                val response = RetrofitClient.apiService.getChats(UserSession.accessToken!!)

                if (response.apiStatus == 200 && response.chats != null) {
                    val decryptedChats = response.chats.map { chat ->
                        // Дешифруем текст последнего сообщения
                        val lastMessage = chat.lastMessage?.let { msg ->
                            val decryptedText = DecryptionUtility.decryptMessage(msg.encryptedText, msg.timeStamp)
                            msg.copy(decryptedText = decryptedText) // Создаем копию с дешифрованным текстом
                        }
                        chat.copy(lastMessage = lastMessage)
                    }
                    _chatList.value = decryptedChats
                } else {
                    // Обработка ошибки
                }
            } catch (e: Exception) {
                // Ошибка сети
            }
        }
    }
    
    // --- SocketManager.SocketListener Implementation ---

    override fun onNewMessage(messageJson: JSONObject) {
        // Эта функция вызывается, когда Socket.IO сервер присылает новое сообщение
        // Обновление списка чатов в ответ на новое сообщение
        fetchChats() 
    }

    override fun onSocketConnected() {
        Log.i("ChatsViewModel", "Socket connected successfully!")
    }

    override fun onSocketDisconnected() {
        Log.w("ChatsViewModel", "Socket disconnected. Attempting to reconnect...")
    }

    override fun onSocketError(error: String) {
        Log.e("ChatsViewModel", "Socket error: $error")
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}