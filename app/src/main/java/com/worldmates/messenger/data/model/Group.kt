package com.worldmates.messenger.data.model

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// ==================== CUSTOM DESERIALIZERS ====================

/**
 * Custom deserializer для last_message, який обробляє як об'єкт так і масив
 * API іноді повертає [] замість null
 */
class LastMessageDeserializer : JsonDeserializer<Message?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Message? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonArray && json.asJsonArray.size() == 0 -> null
            json.isJsonObject -> context?.deserialize(json, Message::class.java)
            else -> null
        }
    }
}

// ==================== BASIC MODELS ====================

data class Chat(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("username") val username: String? = "",
    @SerializedName("avatar") val avatarUrl: String? = "",
    @SerializedName("last_message")
    @JsonAdapter(LastMessageDeserializer::class)
    val lastMessage: Message? = null,
    @SerializedName("message_count") val unreadCount: Int = 0,
    @SerializedName("chat_type") val chatType: String? = "user", // "user", "group", "channel", "private_group"
    @SerializedName("is_group") val isGroup: Boolean = false,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("description") val description: String? = null,
    @SerializedName("members_count") val membersCount: Int = 0,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("pinned_message_id") val pinnedMessageId: Long? = null,
    @SerializedName("last_activity") val lastActivity: Long? = null
)

data class Message(
    @SerializedName("id") val id: Long,
    @SerializedName("from_id") val fromId: Long,
    @SerializedName("to_id") val toId: Long,
    @SerializedName("group_id") val groupId: Long? = null,
    @SerializedName("text") val encryptedText: String?,
    @SerializedName("time") val timeStamp: Long,
    @SerializedName("media") val mediaUrl: String? = null,
    @SerializedName("type") val type: String? = "text", // "text", "image", "video", "audio", "voice", "file", "call"
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("media_duration") val mediaDuration: Long? = null,
    @SerializedName("media_size") val mediaSize: Long? = null,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    @SerializedName("is_edited") val isEdited: Boolean = false,
    @SerializedName("edited_time") val editedTime: Long? = null,
    @SerializedName("is_deleted") val isDeleted: Boolean = false,
    @SerializedName("reply_to_id") val replyToId: Long? = null,
    @SerializedName("reply_to_text") val replyToText: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("read_at") val readAt: Long? = null,
    // Новые поля для AES-GCM шифрования (v2)
    @SerializedName("iv") val iv: String? = null,                          // Base64 Initialization Vector (12 байт)
    @SerializedName("tag") val tag: String? = null,                        // Base64 Authentication Tag (16 байт)
    @SerializedName("cipher_version") val cipherVersion: Int? = null,      // Версия алгоритма (1=ECB, 2=GCM)
    // Реакції емоджі
    @SerializedName("reactions") val reactions: List<MessageReaction>? = null,
    // Локальные поля (не приходят с сервера)
    val decryptedText: String? = null,
    val decryptedMediaUrl: String? = null, // Розшифрований URL медіа (для веб-версії)
    val isLocalPending: Boolean = false // Для локально надісланих повідомлень, що чекають
)

data class Group(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("members_count") val membersCount: Int,
    @SerializedName("admin_id") val adminId: Long,
    @SerializedName("admin_name") val adminName: String,
    @SerializedName("is_private") val isPrivate: Boolean,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_moderator") val isModerator: Boolean = false,
    @SerializedName("is_member") val isMember: Boolean = true,
    @SerializedName("is_muted") val isMuted: Boolean = false, // 🔕 Вимкнено сповіщення для користувача
    @SerializedName("created_time") val createdTime: Long,
    @SerializedName("updated_time") val updatedTime: Long? = null,
    @SerializedName("members") val members: List<GroupMember>? = null,
    @SerializedName("pinned_message_id") val pinnedMessageId: Long? = null,
    @SerializedName("pinned_message") val pinnedMessage: Message? = null, // 📌 Закріплене повідомлення
    @SerializedName("settings") val settings: GroupSettings? = null
)

data class GroupMember(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("role") val role: String, // "admin", "moderator", "member"
    @SerializedName("joined_time") val joinedTime: Long,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("is_blocked") val isBlocked: Boolean = false,
    @SerializedName("permissions") val permissions: List<String>? = null
)

data class GroupSettings(
    @SerializedName("allow_members_invite") val allowMembersInvite: Boolean = true,
    @SerializedName("allow_members_pin") val allowMembersPin: Boolean = false,
    @SerializedName("allow_members_delete_messages") val allowMembersDeleteMessages: Boolean = false,
    @SerializedName("allow_voice_calls") val allowVoiceCalls: Boolean = true,
    @SerializedName("allow_video_calls") val allowVideoCalls: Boolean = true
)

data class MediaFile(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: String, // "image", "video", "audio", "voice", "file"
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("size") val size: Long,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("created_time") val createdTime: Long
)

data class VoiceMessage(
    @SerializedName("id") val id: String,
    val localPath: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("duration") val duration: Long,
    @SerializedName("size") val size: Long,
    @SerializedName("created_time") val createdTime: Long
)

// ==================== API REQUEST MODELS ====================

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isPrivate: Boolean = false,
    val memberIds: List<Long> = emptyList()
)

data class SendMessageRequest(
    val text: String,
    val recipientId: Long? = null,
    val groupId: Long? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val replyToId: Long? = null,
    val sendTime: Long
)

data class CallInitiateRequest(
    val recipientId: Long,
    val callType: String, // "voice", "video"
    val rtcOffer: String? = null,
    val initiatedAt: Long
)

// ==================== API RESPONSE MODELS ====================

data class AuthResponse(
    @SerializedName("api_status") private val _apiStatus: Any?, // Может быть String или Int
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("errors") val errors: ErrorsObject? = null,
    @SerializedName("success_type") val successType: String? = null, // "verification" or "registered"
    @SerializedName("message") val message: String? = null // Success message
) {
    val apiStatus: Int
        get() = when (_apiStatus) {
            is Number -> _apiStatus.toInt()
            is String -> _apiStatus.toIntOrNull() ?: 400
            else -> 400
        }
}

data class ErrorsObject(
    @SerializedName("error_id") val errorId: Int?,
    @SerializedName("error_text") val errorText: String?
)

data class ChatListResponse(
    @SerializedName("api_status") private val _apiStatus: Any?, // Может быть String или Int
    @SerializedName("data") val chats: List<Chat>?,
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("errors") val errors: ErrorsObject? = null
) {
    val apiStatus: Int
        get() = when (_apiStatus) {
            is Number -> _apiStatus.toInt()
            is String -> _apiStatus.toIntOrNull() ?: 400
            else -> 400
        }
}

data class MessageListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("messages") val messages: List<Message>?,
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class GroupListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("groups") private val _groups: List<Group>? = null,
    @SerializedName("data") private val _data: List<Group>? = null, // API get-my-groups возвращает 'data' вместо 'groups'
    @SerializedName("total_count") val totalCount: Int? = null,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
) {
    // Универсальный геттер для получения групп (из groups или data)
    val groups: List<Group>?
        get() = _groups ?: _data
}

data class GroupDetailResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("group") val group: Group?,
    @SerializedName("members") val members: List<GroupMember>? = null,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class MediaUploadResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("media_id") val mediaId: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("size") val size: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class CreateGroupResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("group_id") val groupId: Long?,
    @SerializedName("group") val group: Group?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class CallResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("call_id") val callId: String?,
    @SerializedName("call_token") val callToken: String?,
    @SerializedName("rtc_signal") val rtcSignal: String?,
    @SerializedName("peer_connection_data") val peerConnectionData: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class UserResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("status") val status: String?, // "online", "offline", "away"
    @SerializedName("last_seen") val lastSeen: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class SyncSessionResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("session_id") val sessionId: Long?,
    @SerializedName("platform") val platform: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

// ==================== EXTENSION FUNCTIONS ====================

/**
 * Конвертує Chat об'єкт в Group об'єкт
 * Використовується для отримання груп через getChats API
 */
fun Chat.toGroup(): Group {
    return Group(
        id = this.id,
        name = this.username ?: "Unknown",
        avatarUrl = this.avatarUrl ?: "",
        description = this.description,
        membersCount = this.membersCount,
        adminId = this.userId, // Використовуємо userId як adminId
        adminName = "", // Немає цієї інформації в Chat
        isPrivate = this.isPrivate,
        isAdmin = this.isAdmin,
        isModerator = false, // Немає цієї інформації в Chat
        isMember = true,
        createdTime = this.lastActivity ?: System.currentTimeMillis(),
        updatedTime = this.lastActivity,
        members = null,
        pinnedMessageId = this.pinnedMessageId,
        settings = null
    )
}

/**
 * Extension properties для Group
 */
val Group.isOwner: Boolean
    get() {
        return try {
            adminId == com.worldmates.messenger.data.UserSession.userId
        } catch (e: Exception) {
            false
        }
    }

val Group.lastActivity: Long?
    get() = updatedTime ?: createdTime

/**
 * Extension properties для GroupMember
 */
val GroupMember.isOwner: Boolean
    get() = role == "owner" || role == "admin"

val GroupMember.isAdmin: Boolean
    get() = role == "admin"

val GroupMember.isModerator: Boolean
    get() = role == "moderator"

val GroupMember.isOnline: Boolean
    get() = false // TODO: Implement online status tracking

val GroupMember.avatar: String
    get() = avatarUrl