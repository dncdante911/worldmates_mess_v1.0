package com.worldmates.messenger.network

import android.content.Context
import android.util.Log
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.NetworkQualityMonitor.ConnectionQuality
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

/**
 * 🔄 AdaptiveTransportManager - Адаптивний транспортний менеджер
 *
 * Автоматично переключає між:
 * - Socket.IO (швидке з'єднання)
 * - HTTP Polling (погане з'єднання)
 *
 * В залежності від якості з'єднання відправляє:
 * - Відмінне/добре: повні повідомлення через Socket.IO
 * - Погане: тільки текст через HTTP polling
 */
class AdaptiveTransportManager(
    private val context: Context,
    private val listener: MessageListener
) {
    companion object {
        private const val TAG = "AdaptiveTransport"
        private const val HTTP_POLLING_INTERVAL_MS = 3000L // 3 секунди
    }

    interface MessageListener {
        fun onNewMessage(messageJson: JSONObject)
        fun onConnectionStateChanged(quality: ConnectionQuality)
        fun onTransportChanged(useSocketIO: Boolean)
        fun onError(error: String)
    }

    private val networkMonitor = NetworkQualityMonitor(context)
    private var socketManager: SocketManager? = null
    private var httpPoller: HttpMessagePoller? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isUsingSocketIO = false
    private var lastMessageId = 0L

    init {
        startMonitoring()
    }

    /**
     * Запустити моніторинг та автоматичне перемикання
     */
    private fun startMonitoring() {
        scope.launch {
            networkMonitor.connectionState.collectLatest { state ->
                Log.d(TAG, "📊 Connection state: ${state.quality}, latency: ${state.latencyMs}ms")

                // Повідомляємо слухача про зміну якості
                listener.onConnectionStateChanged(state.quality)

                // Вирішуємо який транспорт використовувати
                val shouldUseSocketIO = networkMonitor.canUseSocketIO()

                if (shouldUseSocketIO != isUsingSocketIO) {
                    switchTransport(shouldUseSocketIO)
                }
            }
        }
    }

    /**
     * Переключити транспорт
     */
    private fun switchTransport(useSocketIO: Boolean) {
        Log.i(TAG, "🔄 Switching transport: ${if (useSocketIO) "Socket.IO" else "HTTP Polling"}")

        if (useSocketIO) {
            // Переключаємось на Socket.IO
            stopHttpPolling()
            startSocketIO()
        } else {
            // Переключаємось на HTTP Polling
            stopSocketIO()
            startHttpPolling()
        }

        isUsingSocketIO = useSocketIO
        listener.onTransportChanged(useSocketIO)
    }

    /**
     * Запустити Socket.IO
     */
    private fun startSocketIO() {
        if (socketManager != null) {
            Log.d(TAG, "Socket.IO вже запущений")
            return
        }

        Log.d(TAG, "🚀 Starting Socket.IO transport")

        socketManager = SocketManager(object : SocketManager.SocketListener {
            override fun onNewMessage(messageJson: JSONObject) {
                listener.onNewMessage(messageJson)

                // Оновлюємо lastMessageId для HTTP fallback
                val messageId = messageJson.optLong("id", 0)
                if (messageId > lastMessageId) {
                    lastMessageId = messageId
                }
            }

            override fun onSocketConnected() {
                Log.d(TAG, "✅ Socket.IO connected")
            }

            override fun onSocketDisconnected() {
                Log.d(TAG, "❌ Socket.IO disconnected")
                // Можливо потрібно перемкнутись на HTTP
                networkMonitor.forceCheck()
            }

            override fun onSocketError(error: String) {
                Log.e(TAG, "❌ Socket.IO error: $error")
                listener.onError(error)
            }
        })

        socketManager?.connect()
    }

    /**
     * Зупинити Socket.IO
     */
    private fun stopSocketIO() {
        Log.d(TAG, "⏹️ Stopping Socket.IO transport")
        socketManager?.disconnect()
        socketManager = null
    }

    /**
     * Запустити HTTP Polling
     */
    private fun startHttpPolling() {
        if (httpPoller != null) {
            Log.d(TAG, "HTTP Polling вже запущений")
            return
        }

        Log.d(TAG, "🚀 Starting HTTP Polling transport")

        httpPoller = HttpMessagePoller(
            pollingInterval = HTTP_POLLING_INTERVAL_MS,
            onNewMessage = { messageJson ->
                listener.onNewMessage(messageJson)

                // Оновлюємо lastMessageId
                val messageId = messageJson.optLong("id", 0)
                if (messageId > lastMessageId) {
                    lastMessageId = messageId
                }
            },
            onError = { error ->
                Log.e(TAG, "❌ HTTP Polling error: $error")
                listener.onError(error)
            }
        )

        httpPoller?.start(lastMessageId)
    }

    /**
     * Зупинити HTTP Polling
     */
    private fun stopHttpPolling() {
        Log.d(TAG, "⏹️ Stopping HTTP Polling transport")
        httpPoller?.stop()
        httpPoller = null
    }

    /**
     * Відправити повідомлення
     */
    fun sendMessage(recipientId: Long, text: String, mediaUrl: String? = null) {
        val quality = networkMonitor.connectionState.value.quality

        // Визначаємо чи включати медіа в payload
        val shouldIncludeMedia = quality in listOf(
            ConnectionQuality.EXCELLENT,
            ConnectionQuality.GOOD
        )

        if (isUsingSocketIO && socketManager != null) {
            // Відправляємо через Socket.IO
            sendViaSocketIO(recipientId, text, if (shouldIncludeMedia) mediaUrl else null)
        } else {
            // Відправляємо через HTTP
            sendViaHttp(recipientId, text, if (shouldIncludeMedia) mediaUrl else null)
        }
    }

    /**
     * Відправити через Socket.IO
     */
    private fun sendViaSocketIO(recipientId: Long, text: String, mediaUrl: String?) {
        socketManager?.sendMessage(recipientId, text)
        // TODO: Додати підтримку mediaUrl в SocketManager.sendMessage()
    }

    /**
     * Відправити через HTTP
     */
    private fun sendViaHttp(recipientId: Long, text: String, mediaUrl: String?) {
        scope.launch {
            try {
                val accessToken = UserSession.accessToken ?: return@launch

                val response = RetrofitClient.apiService.sendMessage(
                    accessToken = accessToken,
                    recipientId = recipientId,
                    text = text,
                    messageHashId = System.currentTimeMillis().toString()
                )

                if (response.apiStatus == 200) {
                    Log.d(TAG, "✅ Message sent via HTTP")
                } else {
                    Log.e(TAG, "❌ Failed to send via HTTP: ${response.apiStatus}")
                    listener.onError("Failed to send message")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ HTTP send error", e)
                listener.onError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Відправити індикатор "печатає"
     */
    fun sendTyping(recipientId: Long, isTyping: Boolean) {
        if (isUsingSocketIO && socketManager != null) {
            socketManager?.sendTyping(recipientId, isTyping)
        }
        // HTTP не підтримує typing indicators
    }

    /**
     * Чи можна завантажувати медіа?
     */
    fun canLoadMedia(): Boolean {
        return networkMonitor.canLoadMedia()
    }

    /**
     * Чи можна завантажувати повне медіа?
     */
    fun canLoadFullMedia(): Boolean {
        return networkMonitor.canLoadFullMedia()
    }

    /**
     * Отримати режим завантаження медіа
     */
    fun getMediaLoadMode(): NetworkQualityMonitor.MediaLoadMode {
        return networkMonitor.connectionState.value.mediaLoadMode
    }

    /**
     * Отримати якість з'єднання
     */
    fun getConnectionQuality(): ConnectionQuality {
        return networkMonitor.connectionState.value.quality
    }

    /**
     * Отримати опис якості
     */
    fun getQualityDescription(): String {
        return networkMonitor.getQualityDescription()
    }

    /**
     * Примусово перевірити якість
     */
    fun forceCheckQuality() {
        networkMonitor.forceCheck()
    }

    /**
     * Очистити ресурси
     */
    fun cleanup() {
        stopSocketIO()
        stopHttpPolling()
        networkMonitor.stopMonitoring()
        scope.cancel()
    }
}

/**
 * 📡 HttpMessagePoller - HTTP polling для отримання нових повідомлень
 */
class HttpMessagePoller(
    private val pollingInterval: Long,
    private val onNewMessage: (JSONObject) -> Unit,
    private val onError: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastFetchedMessageId = 0L

    companion object {
        private const val TAG = "HttpMessagePoller"
    }

    fun start(fromMessageId: Long) {
        lastFetchedMessageId = fromMessageId
        Log.d(TAG, "🚀 Starting HTTP polling from message ID: $lastFetchedMessageId")

        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    fetchNewMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Polling error", e)
                    onError(e.message ?: "Unknown error")
                }

                delay(pollingInterval)
            }
        }
    }

    private suspend fun fetchNewMessages() {
        val accessToken = UserSession.accessToken ?: return

        try {
            // Використовуємо легкий endpoint для polling
            val response = RetrofitClient.apiService.getMessagesLightweight(
                accessToken = accessToken,
                recipientId = 0, // 0 = всі чати
                afterMessageId = lastFetchedMessageId,
                loadMode = "text_only" // Тільки текст
            )

            if (response.apiStatus == 200) {
                val messages = response.messages ?: emptyList()

                if (messages.isNotEmpty()) {
                    Log.d(TAG, "📨 Received ${messages.size} new messages via HTTP polling")

                    messages.forEach { message ->
                        // Конвертуємо в JSONObject для listener
                        val json = JSONObject().apply {
                            put("id", message.id)
                            put("from_id", message.fromId)
                            put("to_id", message.toId)
                            put("text", message.encryptedText)
                            put("timestamp", message.timeStamp)
                            put("type", message.type)
                            // Медіа URL не включаємо - завантажимо окремо
                        }

                        onNewMessage(json)

                        // Оновлюємо lastFetchedMessageId
                        if (message.id > lastFetchedMessageId) {
                            lastFetchedMessageId = message.id
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch messages", e)
            onError(e.message ?: "Unknown error")
        }
    }

    fun stop() {
        Log.d(TAG, "⏹️ Stopping HTTP polling")
        pollingJob?.cancel()
    }
}
