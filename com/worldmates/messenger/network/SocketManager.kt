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
            
            // TODO: Добавить обработку событий is_typing, message_seen и т.д.

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
}