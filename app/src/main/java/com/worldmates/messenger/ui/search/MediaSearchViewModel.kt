package com.worldmates.messenger.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.model.Message
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

/**
 * 🔍 MEDIA SEARCH VIEWMODEL
 *
 * ViewModel для поиска медиа-файлов в чатах
 */
class MediaSearchViewModel : ViewModel() {
    private val TAG = "MediaSearchViewModel"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MediaFilter.ALL)
    val selectedFilter: StateFlow<MediaFilter> = _selectedFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Message>>(emptyList())
    val searchResults: StateFlow<List<Message>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentChatId: Long? = null
    private var currentGroupId: Long? = null

    private val apiService: MediaSearchApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(UserSession.getInstance().getApiUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MediaSearchApiService::class.java)
    }

    /**
     * Установить ID чата или группы
     */
    fun setChatId(chatId: Long?, groupId: Long?) {
        currentChatId = chatId
        currentGroupId = groupId
        Log.d(TAG, "📍 Set chatId=$chatId, groupId=$groupId")
    }

    /**
     * Обновить поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            performSearch()
        } else if (query.isEmpty()) {
            _searchResults.value = emptyList()
        }
    }

    /**
     * Выбрать фильтр
     */
    fun selectFilter(filter: MediaFilter) {
        _selectedFilter.value = filter
        performSearch()
    }

    /**
     * Выполнить поиск
     */
    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.length < 2) {
            Log.d(TAG, "⚠️ Query too short: $query")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔍 Searching: query='$query', filter=${_selectedFilter.value.name}")

                val accessToken = UserSession.getInstance().getAccessToken()
                if (accessToken.isEmpty()) {
                    Log.e(TAG, "❌ No access token")
                    return@launch
                }

                val results = when {
                    currentGroupId != null -> searchInGroup(accessToken, currentGroupId!!, query)
                    currentChatId != null -> searchInChat(accessToken, currentChatId!!, query)
                    else -> {
                        Log.w(TAG, "⚠️ No chat or group ID set")
                        emptyList()
                    }
                }

                // Фильтр по типу медиа
                val filteredResults = if (_selectedFilter.value == MediaFilter.ALL) {
                    results
                } else {
                    results.filter { message ->
                        message.type in _selectedFilter.value.mediaTypes
                    }
                }

                _searchResults.value = filteredResults
                Log.d(TAG, "✅ Found ${filteredResults.size} results (total: ${results.size})")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Search error: ${e.message}", e)
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Поиск в группе
     */
    private suspend fun searchInGroup(accessToken: String, groupId: Long, query: String): List<Message> {
        return try {
            val response = apiService.searchGroupMessages(
                accessToken = accessToken,
                groupId = groupId,
                query = query,
                limit = 100
            )

            if (response.apiStatus == 200) {
                response.messages.orEmpty()
            } else {
                Log.e(TAG, "❌ Group search API error: ${response.errorMessage}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Group search exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Поиск в личном чате
     */
    private suspend fun searchInChat(accessToken: String, chatId: Long, query: String): List<Message> {
        return try {
            val response = apiService.searchChatMessages(
                accessToken = accessToken,
                chatId = chatId,
                query = query,
                limit = 100
            )

            if (response.apiStatus == 200) {
                response.messages.orEmpty()
            } else {
                Log.e(TAG, "❌ Chat search API error: ${response.errorMessage}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Chat search exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Очистить поиск
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}

/**
 * 🌐 API Service для поиска медиа
 */
interface MediaSearchApiService {
    /**
     * Поиск сообщений в группе
     */
    @FormUrlEncoded
    @POST("v2/endpoints/search_group_messages.php")
    suspend fun searchGroupMessages(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("query") query: String,
        @Field("limit") limit: Int = 100,
        @Field("offset") offset: Int = 0
    ): SearchMessagesResponse

    /**
     * Поиск сообщений в личном чате
     */
    @FormUrlEncoded
    @POST("v2/endpoints/search_chat_messages.php")
    suspend fun searchChatMessages(
        @Field("access_token") accessToken: String,
        @Field("chat_id") chatId: Long,
        @Field("query") query: String,
        @Field("limit") limit: Int = 100,
        @Field("offset") offset: Int = 0
    ): SearchMessagesResponse
}

/**
 * 📦 Response для поиска сообщений
 */
data class SearchMessagesResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("messages") val messages: List<Message>? = null,
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null
)
