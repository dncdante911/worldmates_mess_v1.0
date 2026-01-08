// ============ ChannelsViewModel.kt ============

package com.worldmates.messenger.ui.channels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Channel
import com.worldmates.messenger.data.model.CreateChannelRequest
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelsViewModel : ViewModel() {

    private val _channelList = MutableStateFlow<List<Channel>>(emptyList())
    val channelList: StateFlow<List<Channel>> = _channelList

    private val _subscribedChannels = MutableStateFlow<List<Channel>>(emptyList())
    val subscribedChannels: StateFlow<List<Channel>> = _subscribedChannels

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCreatingChannel = MutableStateFlow(false)
    val isCreatingChannel: StateFlow<Boolean> = _isCreatingChannel

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        // Добавляем задержку перед первым запросом
        // чтобы токен успел активироваться на сервере
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500) // 2.5 секунды задержка
            fetchChannels()
            fetchSubscribedChannels()
        }
    }

    /**
     * Завантажує список усіх доступних каналів
     */
    fun fetchChannels() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChannels(
                    accessToken = UserSession.accessToken!!,
                    type = "get_list",
                    limit = 100
                )

                if (response.apiStatus == 200 && response.channels != null) {
                    _channelList.value = response.channels!!
                    _error.value = null
                    Log.d("ChannelsViewModel", "Завантажено ${response.channels!!.size} каналів")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження каналів"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("ChannelsViewModel", "Помилка завантаження каналів", e)
            }
        }
    }

    /**
     * Завантажує список підписаних каналів
     */
    fun fetchSubscribedChannels() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChannels(
                    accessToken = UserSession.accessToken!!,
                    type = "get_subscribed",
                    limit = 100
                )

                if (response.apiStatus == 200 && response.channels != null) {
                    _subscribedChannels.value = response.channels!!
                    Log.d("ChannelsViewModel", "Завантажено ${response.channels!!.size} підписаних каналів")
                } else {
                    Log.w("ChannelsViewModel", "Помилка завантаження підписаних каналів: ${response.errorMessage}")
                }
            } catch (e: Exception) {
                Log.e("ChannelsViewModel", "Помилка завантаження підписаних каналів", e)
            }
        }
    }

    /**
     * Пошук каналів
     */
    fun searchChannels(query: String) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _searchQuery.value = query
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChannels(
                    accessToken = UserSession.accessToken!!,
                    type = "search",
                    query = query,
                    limit = 50
                )

                if (response.apiStatus == 200 && response.channels != null) {
                    _channelList.value = response.channels!!
                    _error.value = null
                    Log.d("ChannelsViewModel", "Знайдено ${response.channels!!.size} каналів за запитом: $query")
                } else {
                    _error.value = response.errorMessage ?: "Помилка пошуку каналів"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("ChannelsViewModel", "Помилка пошуку каналів", e)
            }
        }
    }

    /**
     * Вибір каналу для перегляду
     */
    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
    }

    /**
     * Очистити вибраний канал
     */
    fun clearSelectedChannel() {
        _selectedChannel.value = null
    }

    /**
     * Створює новий канал
     */
    fun createChannel(
        name: String,
        username: String? = null,
        description: String? = null,
        avatarUrl: String? = null,
        isPrivate: Boolean = false,
        category: String? = null,
        onSuccess: (Channel) -> Unit,
        onError: (String) -> Unit
    ) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        if (name.isBlank()) {
            onError("Введіть назву каналу")
            return
        }

        _isCreatingChannel.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.createChannel(
                    accessToken = UserSession.accessToken!!,
                    name = name,
                    username = username,
                    description = description,
                    avatarUrl = avatarUrl,
                    isPrivate = if (isPrivate) 1 else 0,
                    category = category
                )

                if (response.apiStatus == 200 && response.channel != null) {
                    val newChannel = response.channel!!
                    _channelList.value = listOf(newChannel) + _channelList.value
                    _subscribedChannels.value = listOf(newChannel) + _subscribedChannels.value
                    _error.value = null
                    _isCreatingChannel.value = false
                    Log.d("ChannelsViewModel", "Канал створено: ${newChannel.name}")
                    onSuccess(newChannel)
                } else {
                    _error.value = response.errorMessage ?: "Помилка створення каналу"
                    _isCreatingChannel.value = false
                    onError(response.errorMessage ?: "Помилка створення каналу")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                _error.value = errorMsg
                _isCreatingChannel.value = false
                Log.e("ChannelsViewModel", "Помилка створення каналу", e)
                onError(errorMsg)
            }
        }
    }

    /**
     * Підписатися на канал
     */
    fun subscribeChannel(channelId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.subscribeChannel(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200) {
                    // Оновлюємо стан підписки в локальному списку
                    _channelList.value = _channelList.value.map { channel ->
                        if (channel.id == channelId) {
                            channel.copy(
                                isSubscribed = true,
                                subscribersCount = channel.subscribersCount + 1
                            )
                        } else {
                            channel
                        }
                    }

                    // Оновлюємо selected channel
                    _selectedChannel.value?.let { selected ->
                        if (selected.id == channelId) {
                            _selectedChannel.value = selected.copy(
                                isSubscribed = true,
                                subscribersCount = selected.subscribersCount + 1
                            )
                        }
                    }

                    fetchSubscribedChannels() // Оновлюємо список підписаних каналів
                    Log.d("ChannelsViewModel", "Підписка на канал успішна")
                    onSuccess()
                } else {
                    val errorMsg = response.errorMessage ?: "Помилка підписки на канал"
                    Log.e("ChannelsViewModel", errorMsg)
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                Log.e("ChannelsViewModel", "Помилка підписки на канал", e)
                onError(errorMsg)
            }
        }
    }

    /**
     * Відписатися від каналу
     */
    fun unsubscribeChannel(channelId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.unsubscribeChannel(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200) {
                    // Оновлюємо стан підписки в локальному списку
                    _channelList.value = _channelList.value.map { channel ->
                        if (channel.id == channelId) {
                            channel.copy(
                                isSubscribed = false,
                                subscribersCount = maxOf(0, channel.subscribersCount - 1)
                            )
                        } else {
                            channel
                        }
                    }

                    // Оновлюємо selected channel
                    _selectedChannel.value?.let { selected ->
                        if (selected.id == channelId) {
                            _selectedChannel.value = selected.copy(
                                isSubscribed = false,
                                subscribersCount = maxOf(0, selected.subscribersCount - 1)
                            )
                        }
                    }

                    // Видаляємо з підписаних каналів
                    _subscribedChannels.value = _subscribedChannels.value.filter { it.id != channelId }

                    Log.d("ChannelsViewModel", "Відписка від каналу успішна")
                    onSuccess()
                } else {
                    val errorMsg = response.errorMessage ?: "Помилка відписки від каналу"
                    Log.e("ChannelsViewModel", errorMsg)
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                Log.e("ChannelsViewModel", "Помилка відписки від каналу", e)
                onError(errorMsg)
            }
        }
    }

    /**
     * Видалити канал (тільки для адмінів)
     */
    fun deleteChannel(channelId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteChannel(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200) {
                    // Видаляємо канал зі списків
                    _channelList.value = _channelList.value.filter { it.id != channelId }
                    _subscribedChannels.value = _subscribedChannels.value.filter { it.id != channelId }

                    // Очищаємо selected channel якщо це він
                    if (_selectedChannel.value?.id == channelId) {
                        _selectedChannel.value = null
                    }

                    Log.d("ChannelsViewModel", "Канал видалено успішно")
                    onSuccess()
                } else {
                    val errorMsg = response.errorMessage ?: "Помилка видалення каналу"
                    Log.e("ChannelsViewModel", errorMsg)
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                Log.e("ChannelsViewModel", "Помилка видалення каналу", e)
                onError(errorMsg)
            }
        }
    }

    /**
     * Очистити помилку
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Оновити канал після змін
     */
    fun refreshChannel(channelId: Long) {
        if (UserSession.accessToken == null) {
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getChannelDetails(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200 && response.channel != null) {
                    val updatedChannel = response.channel!!

                    // Оновлюємо в загальному списку
                    _channelList.value = _channelList.value.map { channel ->
                        if (channel.id == channelId) updatedChannel else channel
                    }

                    // Оновлюємо в підписаних
                    _subscribedChannels.value = _subscribedChannels.value.map { channel ->
                        if (channel.id == channelId) updatedChannel else channel
                    }

                    // Оновлюємо selected якщо це він
                    if (_selectedChannel.value?.id == channelId) {
                        _selectedChannel.value = updatedChannel
                    }

                    Log.d("ChannelsViewModel", "Канал оновлено: ${updatedChannel.name}")
                }
            } catch (e: Exception) {
                Log.e("ChannelsViewModel", "Помилка оновлення каналу", e)
            }
        }
    }

    /**
     * 🔲 Генерація QR коду для каналу
     */
    fun generateChannelQr(
        channelId: Long,
        onSuccess: (String, String) -> Unit = { _, _ -> }, // qrCode, joinUrl
        onError: (String) -> Unit = {}
    ) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.generateChannelQr(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200 && response.qrCode != null && response.joinUrl != null) {
                    onSuccess(response.qrCode, response.joinUrl)
                    Log.d("ChannelsViewModel", "📡 Channel $channelId QR generated: ${response.qrCode}")
                } else {
                    val errorMsg = response.message ?: "Не вдалося згенерувати QR код"
                    onError(errorMsg)
                    Log.e("ChannelsViewModel", "❌ Failed to generate QR: ${response.message}")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                onError(errorMsg)
                Log.e("ChannelsViewModel", "❌ Error generating QR", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🔲 Підписка на канал за QR кодом
     */
    fun subscribeChannelByQr(
        qrCode: String,
        onSuccess: (com.worldmates.messenger.data.model.Channel) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.subscribeChannelByQr(
                    accessToken = UserSession.accessToken!!,
                    qrCode = qrCode
                )

                if (response.apiStatus == 200 && response.channel != null) {
                    // Оновлюємо список підписаних каналів
                    fetchSubscribedChannels()
                    onSuccess(response.channel)
                    Log.d("ChannelsViewModel", "📡 Subscribed to channel ${response.channel.id} via QR: $qrCode")
                } else {
                    val errorMsg = response.message ?: "Не вдалося підписатися на канал"
                    onError(errorMsg)
                    Log.e("ChannelsViewModel", "❌ Failed to subscribe by QR: ${response.message}")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                onError(errorMsg)
                Log.e("ChannelsViewModel", "❌ Error subscribing by QR", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 📡 Вимкнути сповіщення для каналу
     */
    fun muteChannel(
        channelId: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.muteChannel(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200) {
                    // Оновлюємо деталі каналу
                    refreshChannel(channelId)
                    onSuccess()
                    Log.d("ChannelsViewModel", "📡 Channel $channelId muted")
                } else {
                    val errorMsg = response.message ?: "Не вдалося вимкнути сповіщення"
                    onError(errorMsg)
                    Log.e("ChannelsViewModel", "❌ Failed to mute: ${response.message}")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                onError(errorMsg)
                Log.e("ChannelsViewModel", "❌ Error muting channel", e)
            }
        }
    }

    /**
     * 📡 Увімкнути сповіщення для каналу
     */
    fun unmuteChannel(
        channelId: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (UserSession.accessToken == null) {
            onError("Користувач не авторизований")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.unmuteChannel(
                    accessToken = UserSession.accessToken!!,
                    channelId = channelId
                )

                if (response.apiStatus == 200) {
                    // Оновлюємо деталі каналу
                    refreshChannel(channelId)
                    onSuccess()
                    Log.d("ChannelsViewModel", "📡 Channel $channelId unmuted")
                } else {
                    val errorMsg = response.message ?: "Не вдалося увімкнути сповіщення"
                    onError(errorMsg)
                    Log.e("ChannelsViewModel", "❌ Failed to unmute: ${response.message}")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка: ${e.localizedMessage}"
                onError(errorMsg)
                Log.e("ChannelsViewModel", "❌ Error unmuting channel", e)
            }
        }
    }
}
