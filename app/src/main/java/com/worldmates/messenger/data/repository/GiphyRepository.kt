package com.worldmates.messenger.data.repository

import android.content.Context
import android.util.Log
import com.giphy.sdk.core.GPHCore
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.network.api.CompletionHandler
import com.giphy.sdk.core.network.api.GPHApiClient
import com.giphy.sdk.core.network.response.ListMediaResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 🎬 GIPHY Repository - работа с GIPHY API
 *
 * Функции:
 * - Поиск GIF по запросу
 * - Trending GIF (популярные)
 * - Получение случайного GIF
 *
 * Использование:
 * ```
 * val giphyRepo = GiphyRepository.getInstance(context)
 * val trendingGifs = giphyRepo.fetchTrendingGifs()
 * val searchResults = giphyRepo.searchGifs("funny cats")
 * ```
 */
class GiphyRepository private constructor(
    private val context: Context
) {
    companion object {
        @Volatile
        private var instance: GiphyRepository? = null

        fun getInstance(context: Context): GiphyRepository {
            return instance ?: synchronized(this) {
                instance ?: GiphyRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private const val TAG = "GiphyRepository"

        // GIPHY API Key
        // TODO: Заменить на реальный ключ от https://developers.giphy.com/
        // ВАЖНО: В продакшене хранить в BuildConfig или secure storage!
        private const val GIPHY_API_KEY = "YOUR_GIPHY_API_KEY_HERE"
    }

    // GIPHY API Client
    private val apiClient: GPHApiClient by lazy {
        // Инициализируем GIPHY SDK
        GPHCore.configure(context, GIPHY_API_KEY)
        GPHApiClient(GIPHY_API_KEY)
    }

    // State flows для кэширования
    private val _trendingGifs = MutableStateFlow<List<Media>>(emptyList())
    val trendingGifs: StateFlow<List<Media>> = _trendingGifs

    private val _searchResults = MutableStateFlow<List<Media>>(emptyList())
    val searchResults: StateFlow<List<Media>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * 🔥 Получить Trending GIF (популярные)
     */
    suspend fun fetchTrendingGifs(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Media>> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true

            val result = suspendCoroutine<Result<List<Media>>> { continuation ->
                apiClient.trending(
                    com.giphy.sdk.core.models.enums.MediaType.gif,
                    limit,
                    offset,
                    null,
                    null,
                    object : CompletionHandler<ListMediaResponse> {
                        override fun onComplete(
                            result: ListMediaResponse?,
                            e: Throwable?
                        ) {
                            if (e != null) {
                                Log.e(TAG, "Trending GIFs error", e)
                                continuation.resume(Result.failure(e))
                            } else if (result?.data != null) {
                                _trendingGifs.value = result.data ?: emptyList()
                                continuation.resume(Result.success(result.data ?: emptyList()))
                            } else {
                                continuation.resume(Result.success(emptyList()))
                            }
                        }
                    }
                )
            }

            _isLoading.value = false
            result
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "fetchTrendingGifs error", e)
            Result.failure(e)
        }
    }

    /**
     * 🔍 Поиск GIF по запросу
     */
    suspend fun searchGifs(
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Media>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            _isLoading.value = true

            val result = suspendCoroutine<Result<List<Media>>> { continuation ->
                apiClient.search(
                    query,
                    com.giphy.sdk.core.models.enums.MediaType.gif,
                    limit,
                    offset,
                    null,
                    null,
                    null,
                    object : CompletionHandler<ListMediaResponse> {
                        override fun onComplete(
                            result: ListMediaResponse?,
                            e: Throwable?
                        ) {
                            if (e != null) {
                                Log.e(TAG, "Search GIFs error: $query", e)
                                continuation.resume(Result.failure(e))
                            } else if (result?.data != null) {
                                _searchResults.value = result.data ?: emptyList()
                                continuation.resume(Result.success(result.data ?: emptyList()))
                            } else {
                                continuation.resume(Result.success(emptyList()))
                            }
                        }
                    }
                )
            }

            _isLoading.value = false
            result
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "searchGifs error: $query", e)
            Result.failure(e)
        }
    }

    /**
     * 🎲 Получить случайный GIF
     */
    suspend fun fetchRandomGif(
        tag: String? = null
    ): Result<Media?> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true

            val result = suspendCoroutine<Result<Media?>> { continuation ->
                apiClient.random(
                    tag,
                    com.giphy.sdk.core.models.enums.MediaType.gif,
                    null,
                    object : CompletionHandler<com.giphy.sdk.core.network.response.MediaResponse> {
                        override fun onComplete(
                            result: com.giphy.sdk.core.network.response.MediaResponse?,
                            e: Throwable?
                        ) {
                            if (e != null) {
                                Log.e(TAG, "Random GIF error", e)
                                continuation.resume(Result.failure(e))
                            } else {
                                continuation.resume(Result.success(result?.data))
                            }
                        }
                    }
                )
            }

            _isLoading.value = false
            result
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e(TAG, "fetchRandomGif error", e)
            Result.failure(e)
        }
    }

    /**
     * 📊 Получить URL GIF в разных качествах
     */
    fun getGifUrls(media: Media): GifUrls {
        return GifUrls(
            original = media.images?.original?.gifUrl ?: "",
            downsized = media.images?.downsized?.gifUrl ?: "",
            downsizedMedium = media.images?.downsizedMedium?.gifUrl ?: "",
            downsizedLarge = media.images?.downsizedLarge?.gifUrl ?: "",
            preview = media.images?.previewGif?.gifUrl ?: "",
            fixedWidth = media.images?.fixedWidth?.gifUrl ?: "",
            fixedHeight = media.images?.fixedHeight?.gifUrl ?: ""
        )
    }

    /**
     * Очистить кэш
     */
    fun clearCache() {
        _trendingGifs.value = emptyList()
        _searchResults.value = emptyList()
    }
}

/**
 * GIF URLs в разных качествах
 */
data class GifUrls(
    val original: String,           // Оригинальное качество (большой размер)
    val downsized: String,           // Сжатый (маленький)
    val downsizedMedium: String,     // Сжатый средний
    val downsizedLarge: String,      // Сжатый большой
    val preview: String,             // Превью (очень маленький)
    val fixedWidth: String,          // Фиксированная ширина
    val fixedHeight: String          // Фиксированная высота
) {
    /**
     * Получить лучший URL для отправки в чат
     * (баланс между качеством и размером)
     */
    fun getBestForChat(): String {
        return downsizedMedium.ifEmpty {
            downsized.ifEmpty {
                fixedWidth.ifEmpty {
                    original
                }
            }
        }
    }

    /**
     * Получить URL для превью
     */
    fun getPreview(): String {
        return preview.ifEmpty {
            downsized.ifEmpty {
                fixedWidth
            }
        }
    }
}
