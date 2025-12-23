package com.worldmates.messenger.network

import android.util.Log
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * Менеджер для Socket.IO, обрабатывающий подключение к Node.js чат-серверу.
 */
class SocketManager(private val listener: SocketListener) {

    private var socket: Socket? = null

    interface SocketListener {
        fun onNewMessage(messageJson: JSONObject)
        fun onSocketConnected()
        fun onSocketDisconnected()
        fun onSocketError(error: String)
    }

    fun connect() {
        if (UserSession.accessToken == null || socket?.connected() == true) return

        try {
            // Опции для Socket.IO с улучшенными настройками переподключения
            val opts = IO.Options()
            opts.forceNew = false // Не создавать новое соединение, если уже есть
            opts.reconnection = true // Автоматическое переподключение
            opts.reconnectionAttempts = Int.MAX_VALUE // Бесконечные попытки переподключения
            opts.reconnectionDelay = 1000 // Начальная задержка 1 сек
            opts.reconnectionDelayMax = 5000 // Максимальная задержка 5 сек
            opts.timeout = 20000 // Тайм-аут подключения 20 сек
            opts.query = "access_token=${UserSession.accessToken}&user_id=${UserSession.userId}"

            socket = IO.socket(Constants.SOCKET_URL, opts)

            // 1. Обработка подключения
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Socket Connected! ID: ${socket?.id()}")
                // Отправляем событие аутентификации для привязки сокета к пользователю
                authenticateSocket()
                listener.onSocketConnected()
            }

            // 2. Обработка отключения
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketManager", "Socket Disconnected")
                listener.onSocketDisconnected()
            }

            // 3. Обработка переподключения
            socket?.on("reconnect") {
                Log.d("SocketManager", "Socket Reconnected")
                authenticateSocket()
                listener.onSocketConnected()
            }

            // 4. Обработка попытки переподключения
            socket?.on("reconnecting") { args ->
                val attempt = if (args.isNotEmpty()) args[0].toString() else "?"
                Log.d("SocketManager", "Reconnection Attempt #$attempt")
            }

            // 6. Обработка ошибок
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                Log.e("SocketManager", "Connection Error: $error")
                listener.onSocketError("Connection Error: $error")
            }

            // 7. Получение нового личного сообщения (основное событие от сервера)
            socket?.on(Constants.SOCKET_EVENT_PRIVATE_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val messageData = args[0] as JSONObject
                    Log.d("SocketManager", "Received private_message: ${messageData.toString()}")
                    listener.onNewMessage(messageData)
                }
            }

            // 8. Получение нового сообщения (для обратной совместимости)
            socket?.on(Constants.SOCKET_EVENT_NEW_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    Log.d("SocketManager", "Received new_message: ${args[0]}")
                    listener.onNewMessage(args[0] as JSONObject)
                }
            }

            // 8. Обработка индикатора печатания
            socket?.on(Constants.SOCKET_EVENT_TYPING) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    // Сервер отправляет sender_id (НЕ user_id!) и is_typing: 200 (печатает) или 300 (закончил)
                    val senderId = data.optLong("sender_id", 0)
                    val isTypingCode = data.optInt("is_typing", 0)
                    val isTyping = isTypingCode == 200  // 200 = печатает, 300 = закончил
                    Log.d("SocketManager", "User $senderId is typing: $isTyping (code: $isTypingCode)")
                    if (listener is ExtendedSocketListener) {
                        listener.onTypingStatus(senderId, isTyping)
                    }
                }
            }

            // 9. Обработка "последний раз в сети"
            socket?.on(Constants.SOCKET_EVENT_LAST_SEEN) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val userId = data.optLong("user_id", 0)
                    val lastSeen = data.optLong("last_seen", 0)
                    Log.d("SocketManager", "User $userId last seen: $lastSeen")
                    if (listener is ExtendedSocketListener) {
                        listener.onLastSeen(userId, lastSeen)
                    }
                }
            }

            // 10. Обработка прочтения сообщения
            socket?.on(Constants.SOCKET_EVENT_MESSAGE_SEEN) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val messageId = data.optLong("message_id", 0)
                    val userId = data.optLong("user_id", 0)
                    Log.d("SocketManager", "Message $messageId seen by user $userId")
                    if (listener is ExtendedSocketListener) {
                        listener.onMessageSeen(messageId, userId)
                    }
                }
            }

            // 11. Обработка группового сообщения
            socket?.on(Constants.SOCKET_EVENT_GROUP_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    Log.d("SocketManager", "Group message received")
                    if (listener is ExtendedSocketListener) {
                        listener.onGroupMessage(data)
                    }
                }
            }

            // 12. Обработка статуса "онлайн"
            socket?.on(Constants.SOCKET_EVENT_USER_ONLINE) { args ->
                Log.d("SocketManager", "Received ${Constants.SOCKET_EVENT_USER_ONLINE} event with ${args.size} args")
                if (args.isNotEmpty()) {
                    Log.d("SocketManager", "Event data: ${args[0]}")
                    if (args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val userId = data.optLong("user_id", 0)
                        Log.d("SocketManager", "✅ User $userId is ONLINE")
                        if (listener is ExtendedSocketListener) {
                            listener.onUserOnline(userId)
                        }
                    }
                }
            }

            // 13. Обработка статуса "оффлайн"
            socket?.on(Constants.SOCKET_EVENT_USER_OFFLINE) { args ->
                Log.d("SocketManager", "Received ${Constants.SOCKET_EVENT_USER_OFFLINE} event with ${args.size} args")
                if (args.isNotEmpty()) {
                    Log.d("SocketManager", "Event data: ${args[0]}")
                    if (args[0] is JSONObject) {
                        val data = args[0] as JSONObject
                        val userId = data.optLong("user_id", 0)
                        Log.d("SocketManager", "❌ User $userId is OFFLINE")
                        if (listener is ExtendedSocketListener) {
                            listener.onUserOffline(userId)
                        }
                    }
                }
            }

            socket?.connect()

        } catch (e: Exception) {
            e.printStackTrace()
            listener.onSocketError("Socket Connection Exception: ${e.message}")
        }
    }

    private fun authenticateSocket() {
        // Проверяем токен и отправляем данные для "привязки" сокета на Node.js
        // Сервер ожидает событие "join" с session hash в поле user_id
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val authData = JSONObject().apply {
                // user_id должен быть session hash (access_token), а НЕ числовой ID
                put("user_id", UserSession.accessToken)
                // Опционально: можно добавить массивы открытых чатов
                // put("recipient_ids", JSONArray())
                // put("recipient_group_ids", JSONArray())
            }
            socket?.emit(Constants.SOCKET_EVENT_AUTH, authData)
            Log.d("SocketManager", "Sent 'join' event with session hash: ${UserSession.accessToken?.take(10)}...")
        }
    }

    fun sendMessage(recipientId: Long, text: String) {
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val messagePayload = JSONObject().apply {
                // Сервер ожидает именно эти поля (см. PrivateMessageController.js)
                put("msg", text)  // НЕ "text"!
                put("from_id", UserSession.userId)  // НЕ "user_id"!
                put("to_id", recipientId)  // НЕ "recipient_id"!
                // TODO: Добавить поля для медиа, стикеров и т.д.
                // mediaId, mediaFilename, record, message_reply_id, story_id, lng, lat, contact, color, isSticker
            }
            socket?.emit(Constants.SOCKET_EVENT_SEND_MESSAGE, messagePayload)
            Log.d("SocketManager", "Emitted private_message to user $recipientId: $text")
        } else {
            // Fallback: Если Socket не подключен, можно использовать REST API для отправки (send-message.php)
            Log.w("SocketManager", "Socket not connected. Message not sent via socket.")
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }

    /**
     * Универсальный метод для отправки произвольных событий через Socket.IO
     * Используется для WebRTC сигнализации и других кастомных событий
     */
    fun emit(event: String, data: Any) {
        if (socket?.connected() == true) {
            socket?.emit(event, data)
            Log.d("SocketManager", "Emitted event: $event")
        } else {
            Log.w("SocketManager", "Cannot emit event '$event': Socket not connected")
        }
    }

    /**
     * Отправляет индикатор "печатает"
     */
    fun sendTyping(recipientId: Long, isTyping: Boolean) {
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val typingPayload = JSONObject().apply {
                put("access_token", UserSession.accessToken)
                put("user_id", UserSession.userId)
                put("recipient_id", recipientId)
                put("is_typing", isTyping)
            }
            socket?.emit(Constants.SOCKET_EVENT_TYPING, typingPayload)
        }
    }

    /**
     * Отправляет подтверждение прочтения сообщения
     */
    fun sendMessageSeen(messageId: Long, senderId: Long) {
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val seenPayload = JSONObject().apply {
                put("access_token", UserSession.accessToken)
                put("user_id", UserSession.userId)
                put("message_id", messageId)
                put("sender_id", senderId)
            }
            socket?.emit(Constants.SOCKET_EVENT_MESSAGE_SEEN, seenPayload)
        }
    }

    /**
     * Отправляет групповое сообщение
     */
    fun sendGroupMessage(groupId: Long, text: String) {
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val messagePayload = JSONObject().apply {
                // Сервер ожидает именно эти поля (см. GroupMessageController.js)
                put("msg", text)  // НЕ "text"!
                put("from_id", UserSession.userId)  // НЕ "user_id"!
                put("group_id", groupId)
                // TODO: mediaId, message_reply_id, color, isSticker
            }
            socket?.emit(Constants.SOCKET_EVENT_GROUP_MESSAGE, messagePayload)
            Log.d("SocketManager", "Emitted group_message to group $groupId: $text")
        }
    }

    /**
     * Расширенный интерфейс для дополнительных событий
     */
    interface ExtendedSocketListener : SocketListener {
        fun onTypingStatus(userId: Long, isTyping: Boolean) {}
        fun onLastSeen(userId: Long, lastSeen: Long) {}
        fun onMessageSeen(messageId: Long, userId: Long) {}
        fun onGroupMessage(messageJson: JSONObject) {}
        fun onUserOnline(userId: Long) {}
        fun onUserOffline(userId: Long) {}
    }
}