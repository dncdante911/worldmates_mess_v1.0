package com.worldmates.messenger.services

import android.app.*
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.ui.messages.MessagesActivity
import com.worldmates.messenger.ui.calls.IncomingCallActivity
import com.worldmates.messenger.ui.chats.ChatsActivity
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * Foreground Service that listens to Socket.IO for new messages
 * and shows notifications when the app is in background.
 *
 * This provides push-like notifications without Firebase.
 */
class MessageNotificationService : Service() {

    companion object {
        private const val TAG = "MsgNotifService"
        private const val CHANNEL_ID = "wm_message_notifications"
        private const val CHANNEL_NAME = "Повідомлення"
        private const val FOREGROUND_NOTIFICATION_ID = 9001
        private const val SERVICE_CHANNEL_ID = "wm_service_channel"
        private const val SERVICE_CHANNEL_NAME = "WorldMates Service"

        var isRunning = false
            private set

        // Track which chat is currently open so we don't show notifications for it
        var activeRecipientId: Long = 0
        var activeGroupId: Long = 0

        fun start(context: Context) {
            if (isRunning) return
            val intent = Intent(context, MessageNotificationService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MessageNotificationService::class.java)
            context.stopService(intent)
        }
    }

    private var socket: Socket? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannels()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
        connectSocket()
        Log.d(TAG, "MessageNotificationService started")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        socket?.disconnect()
        socket?.off()
        socket = null
        Log.d(TAG, "MessageNotificationService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun connectSocket() {
        val accessToken = UserSession.accessToken
        if (accessToken.isNullOrEmpty()) {
            Log.e(TAG, "No access token, cannot connect")
            stopSelf()
            return
        }

        try {
            val opts = IO.Options().apply {
                forceNew = false
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 2000
                reconnectionDelayMax = 10000
                timeout = 20000
                transports = arrayOf("websocket", "polling")
                query = "access_token=$accessToken&user_id=${UserSession.userId}"
            }

            socket = IO.socket(Constants.SOCKET_URL, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Notification socket connected")
                // Authenticate
                val authData = JSONObject().apply {
                    put("user_id", accessToken)
                }
                socket?.emit(Constants.SOCKET_EVENT_AUTH, authData)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Notification socket disconnected")
            }

            // Listen for private messages
            socket?.on(Constants.SOCKET_EVENT_PRIVATE_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingMessage(args[0] as JSONObject, isGroup = false)
                }
            }

            socket?.on(Constants.SOCKET_EVENT_NEW_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingMessage(args[0] as JSONObject, isGroup = false)
                }
            }

            // Additional private-message events used by backend variants
            socket?.on("private_message_page") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingMessage(args[0] as JSONObject, isGroup = false)
                }
            }

            socket?.on("page_message") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingMessage(args[0] as JSONObject, isGroup = false)
                }
            }

            // Listen for group messages
            socket?.on(Constants.SOCKET_EVENT_GROUP_MESSAGE) { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingMessage(args[0] as JSONObject, isGroup = true)
                }
            }

            // Listen for incoming calls
            socket?.on("call:incoming") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    handleIncomingCall(args[0] as JSONObject)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection error", e)
        }
    }

    private fun handleIncomingMessage(data: JSONObject, isGroup: Boolean) {
        try {
            Log.d(TAG, "📨 handleIncomingMessage called: isGroup=$isGroup, data=$data")

            val senderId = data.optLong("from_id", data.optLong("sender_id", 0))
            val senderName = data.optString("sender_name",
                data.optString("from_name", "WorldMates"))
            val text = data.optString("msg",
                data.optString("text",
                    data.optString("message", "")))
            val groupId = data.optLong("group_id", 0)

            Log.d(TAG, "   senderId=$senderId, senderName=$senderName, text=${text.take(30)}")
            Log.d(TAG, "   UserSession.userId=${UserSession.userId}")
            Log.d(TAG, "   activeRecipientId=$activeRecipientId, activeGroupId=$activeGroupId")

            // Don't show notification for our own messages
            if (senderId == UserSession.userId) {
                Log.d(TAG, "   ❌ Skipping: own message")
                return
            }

            // Don't show notification if the user is viewing this exact chat
            if (!isGroup && senderId == activeRecipientId && activeRecipientId > 0) {
                Log.d(TAG, "   ❌ Skipping: chat is open")
                return
            }
            if (isGroup && groupId == activeGroupId && activeGroupId > 0) {
                Log.d(TAG, "   ❌ Skipping: group chat is open")
                return
            }

            // Don't notify for empty messages
            if (text.isBlank()) {
                Log.d(TAG, "   ❌ Skipping: empty text")
                return
            }

            val title = if (isGroup) {
                val groupName = data.optString("group_name", "Група")
                "$senderName @ $groupName"
            } else {
                senderName
            }

            showMessageNotification(
                title = title,
                message = text,
                senderId = senderId,
                senderName = senderName,
                groupId = groupId,
                isGroup = isGroup
            )

            Log.d(TAG, "Notification shown: $title: ${text.take(30)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message for notification", e)
        }
    }

    private fun handleIncomingCall(data: JSONObject) {
        try {
            val fromId = data.optInt("fromId", data.optInt("from_id", 0))
            val fromName = data.optString("fromName", data.optString("from_name", "WorldMates"))
            val fromAvatar = data.optString("fromAvatar", data.optString("from_avatar", ""))
            val callType = data.optString("callType", data.optString("call_type", "audio"))
            val roomName = data.optString("roomName", data.optString("room_name", ""))
            val sdpOffer = data.optString("sdpOffer", data.optString("sdp_offer", null))

            // Don't show notification for our own calls
            if (fromId.toLong() == UserSession.userId) return

            // Launch IncomingCallActivity
            val intent = IncomingCallActivity.createIntent(
                context = this,
                fromId = fromId,
                fromName = fromName,
                fromAvatar = fromAvatar,
                callType = callType,
                roomName = roomName,
                sdpOffer = sdpOffer
            )
            startActivity(intent)

            // Also show notification
            showCallNotification(
                fromName = fromName,
                callType = callType,
                fromId = fromId
            )

            Log.d(TAG, "📞 Incoming call notification: $fromName ($callType)")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming call notification", e)
        }
    }

    private fun showCallNotification(
        fromName: String,
        callType: String,
        fromId: Int
    ) {
        val callTypeText = if (callType == "video") "Відеодзвінок" else "Аудіодзвінок"

        // Intent для відкриття списку дзвінків при кліку
        val intent = Intent(this, com.worldmates.messenger.ui.calls.CallHistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            fromId + 50000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$callTypeText від $fromName")
            .setContentText("Натисніть, щоб переглянути історію дзвінків")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$callTypeText від $fromName"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setFullScreenIntent(pendingIntent, true) // For incoming calls
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(fromId + 50000, notification)
    }

    private fun showMessageNotification(
        title: String,
        message: String,
        senderId: Long,
        senderName: String,
        groupId: Long,
        isGroup: Boolean
    ) {
        Log.d(TAG, "🔔 showMessageNotification: title=$title, message=${message.take(30)}")

        if (!canPostNotifications()) {
            Log.w(TAG, "❌ Notifications are disabled or permission is missing")
            return
        }

        Log.d(TAG, "✅ Notification permission granted, creating notification")

        val intent = Intent(this, MessagesActivity::class.java).apply {
            if (isGroup) {
                putExtra("group_id", groupId)
            } else {
                putExtra("recipient_id", senderId)
                putExtra("recipient_name", senderName)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId = if (isGroup) (groupId + 100000).toInt() else senderId.toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    private fun canPostNotifications(): Boolean {
        val enabledBySystem = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!enabledBySystem) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildForegroundNotification(): Notification {
        // Intent to open ChatsActivity when clicked
        val intent = Intent(this, ChatsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("WorldMates")
            .setContentText("Очікуємо нові повідомлення")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Delete old channels to recreate with correct settings
        try {
            nm.deleteNotificationChannel(CHANNEL_ID)
            nm.deleteNotificationChannel(SERVICE_CHANNEL_ID)
            Log.d(TAG, "🗑️ Deleted old notification channels")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete old channels: ${e.message}")
        }

        // Message notifications channel (HIGH priority with sound and vibration)
        val messageChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Нові повідомлення від WorldMates"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 100, 250)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            Log.d(TAG, "🔔 Message channel created with sound: $soundUri")
        }
        nm.createNotificationChannel(messageChannel)

        // Service channel (low priority, silent)
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            SERVICE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Фонова служба WorldMates"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        nm.createNotificationChannel(serviceChannel)

        Log.d(TAG, "✅ Notification channels created successfully")
    }
}
