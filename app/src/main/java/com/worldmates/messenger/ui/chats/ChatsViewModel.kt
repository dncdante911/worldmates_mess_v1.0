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

    private val _needsRelogin = MutableStateFlow(false)
    val needsRelogin: StateFlow<Boolean> = _needsRelogin

    private var socketManager: SocketManager? = null
    private var authErrorCount = 0 // Счетчик ошибок авторизации

    init {
        // Добавляем задержку перед первым запросом
        // чтобы токен успел активироваться на сервере
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 2 секунды задержка
            fetchChats()
        }
        setupSocket()
    }

    /**
     * Завантажує список чатів з сервера
     */
    fun fetchChats() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            Log.e("ChatsViewModel", "Access token is null!")
            return
        }

        Log.d("ChatsViewModel", "Початок завантаження чатів...")
        Log.d("ChatsViewModel", "Access token: ${UserSession.accessToken}")
        Log.d("ChatsViewModel", "User ID: ${UserSession.userId}")

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Виклик API для отримання чатів
                val response = RetrofitClient.apiService.getChats(
                    accessToken = UserSession.accessToken!!,
                    limit = 50,
                    dataType = "all",
                    setOnline = 1
                )

                Log.d("ChatsViewModel", "API Response Status: ${response.apiStatus}")
                Log.d("ChatsViewModel", "Chats count: ${response.chats?.size ?: 0}")
                Log.d("ChatsViewModel", "Error code: ${response.errorCode}")
                Log.d("ChatsViewModel", "Error message: ${response.errorMessage}")

                if (response.apiStatus == 200) {
                    if (response.chats != null && response.chats.isNotEmpty()) {
                        Log.d("ChatsViewModel", "Отримано ${response.chats.size} чатів")

                        // Дешифруємо останнє повідомлення у кожному чаті
                        val decryptedChats = response.chats.map { chat ->
                            Log.d("ChatsViewModel", "Chat: ${chat.username}, last_msg: ${chat.lastMessage?.encryptedText}")

                            val lastMessage = chat.lastMessage?.let { msg ->
                                val decryptedText = DecryptionUtility.decryptMessageOrOriginal(
                                    msg.encryptedText,
                                    msg.timeStamp
                                )
                                Log.d("ChatsViewModel", "🔐 Дешифрування для ${chat.username}:")
                                Log.d("ChatsViewModel", "   Зашифровано: ${msg.encryptedText}")
                                Log.d("ChatsViewModel", "   Timestamp: ${msg.timeStamp}")
                                Log.d("ChatsViewModel", "   Дешифровано: $decryptedText")
                                msg.copy(decryptedText = decryptedText)
                            }
                            chat.copy(lastMessage = lastMessage)
                        }

                        _chatList.value = decryptedChats
                        _error.value = null
                        Log.d("ChatsViewModel", "✅ Завантажено ${decryptedChats.size} чатів успішно")
                    } else {
                        Log.w("ChatsViewModel", "⚠️ API повернуло 200, але чатів немає")
                        _chatList.value = emptyList()
                        _error.value = null // Не помилка, просто порожньо
                    }
                } else {
                    // Якщо отримали 404 або помилку авторизації - очищаємо сесію і вимагаємо перелогін
                    if (response.apiStatus == 404 || response.apiStatus == 401 || response.apiStatus == 403) {
                        Log.e("ChatsViewModel", "❌ Токен недійсний або застарілий. Потрібен перелогін")
                        UserSession.clearSession()
                        _needsRelogin.value = true
                        _error.value = "Сесія застаріла. Будь ласка, увійдіть знову"
                    } else {
                        val errorMsg = response.errorMessage ?: "Невідома помилка (${response.apiStatus})"
                        _error.value = errorMsg
                        Log.e("ChatsViewModel", "❌ Помилка API: ${response.apiStatus} - $errorMsg")
                    }
                }

                _isLoading.value = false
            } catch (e: com.google.gson.JsonSyntaxException) {
                val errorMsg = "Помилка парсингу відповіді від сервера"
                _error.value = errorMsg
                _isLoading.value = false
                Log.e("ChatsViewModel", "❌ $errorMsg", e)
            } catch (e: java.net.ConnectException) {
                val errorMsg = "Не вдалося з'єднатися з сервером"
                _error.value = errorMsg
                _isLoading.value = false
                Log.e("ChatsViewModel", "❌ $errorMsg", e)
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                _error.value = errorMsg
                _isLoading.value = false
                Log.e("ChatsViewModel", "❌ Помилка завантаження чатів", e)
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
        // Не показуємо помилку користувачу, оскільки Socket.IO автоматично спробує переподключитися
        // _error.value = "Втрачено з'єднання"

        // Спробуємо переподключитися через 2 секунди, якщо автоматичне переподключення не спрацює
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            if (socketManager != null) {
                try {
                    socketManager?.connect()
                    Log.d("ChatsViewModel", "Спроба переподключення Socket...")
                } catch (e: Exception) {
                    Log.e("ChatsViewModel", "Помилка переподключення Socket", e)
                }
            }
        }
    }

    override fun onSocketError(error: String) {
        Log.e("ChatsViewModel", "Помилка Socket: $error")
        // Не показуємо помилку Socket користувачу, якщо це тимчасова помилка з'єднання
        if (!error.contains("xhr poll error", ignoreCase = true) &&
            !error.contains("timeout", ignoreCase = true)) {
            _error.value = error
        }
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