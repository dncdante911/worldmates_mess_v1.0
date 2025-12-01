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
            // Опции для Socket.IO (могут потребовать доработки в зависимости от конфигурации вашего сервера)
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
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

            // 3. Обработка ошибок
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                Log.e("SocketManager", "Connection Error: $error")
                listener.onSocketError("Connection Error: $error")
            }

            // 4. Получение нового сообщения
            socket?.on(Constants.SOCKET_EVENT_NEW_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    listener.onNewMessage(args[0] as JSONObject)
                }
            }

            // 5. Обработка индикатора печатания
            socket?.on(Constants.SOCKET_EVENT_TYPING) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val userId = data.optLong("user_id", 0)
                    val isTyping = data.optBoolean("is_typing", false)
                    Log.d("SocketManager", "User $userId is typing: $isTyping")
                    if (listener is ExtendedSocketListener) {
                        listener.onTypingStatus(userId, isTyping)
                    }
                }
            }

            // 6. Обработка "последний раз в сети"
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

            // 7. Обработка прочтения сообщения
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

            // 8. Обработка группового сообщения
            socket?.on(Constants.SOCKET_EVENT_GROUP_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    Log.d("SocketManager", "Group message received")
                    if (listener is ExtendedSocketListener) {
                        listener.onGroupMessage(data)
                    }
                }
            }

            // 9. Обработка статуса "онлайн"
            socket?.on(Constants.SOCKET_EVENT_USER_ONLINE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val userId = data.optLong("user_id", 0)
                    Log.d("SocketManager", "User $userId is online")
                    if (listener is ExtendedSocketListener) {
                        listener.onUserOnline(userId)
                    }
                }
            }

            // 10. Обработка статуса "оффлайн"
            socket?.on(Constants.SOCKET_EVENT_USER_OFFLINE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val userId = data.optLong("user_id", 0)
                    Log.d("SocketManager", "User $userId is offline")
                    if (listener is ExtendedSocketListener) {
                        listener.onUserOffline(userId)
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
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val authData = JSONObject().apply {
                put("access_token", UserSession.accessToken)
                put("user_id", UserSession.userId)
            }
            socket?.emit(Constants.SOCKET_EVENT_AUTH, authData)
            Log.d("SocketManager", "Sent authentication data to Node.js server.")
        }
    }

    fun sendMessage(recipientId: Long, text: String) {
        if (socket?.connected() == true && UserSession.accessToken != null) {
            val messagePayload = JSONObject().apply {
                put("access_token", UserSession.accessToken)
                put("user_id", UserSession.userId)
                put("recipient_id", recipientId)
                put("text", text)
                // TODO: Добавить поля для медиа, стикеров и т.д.
            }
            socket?.emit(Constants.SOCKET_EVENT_SEND_MESSAGE, messagePayload)
        } else {
            // Fallback: Если Socket не подключен, можно использовать REST API для отправки (send-message.php)
            Log.w("SocketManager", "Socket not connected. Message not sent via socket.")
        }
    }

    fun disconnect() {
        socket?.disconnect()
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
                put("access_token", UserSession.accessToken)
                put("user_id", UserSession.userId)
                put("group_id", groupId)
                put("text", text)
            }
            socket?.emit(Constants.SOCKET_EVENT_GROUP_MESSAGE, messagePayload)
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