package com.worldmates.messenger.data.model

import com.google.gson.annotations.SerializedName

// ==================== CHAT MODELS ====================

data class Chat(
    @SerializedName("id") val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("last_message") val lastMessage: Message?,
    @SerializedName("message_count") val unreadCount: Int,
    @SerializedName("chat_type") val chatType: String, // "user", "group", "channel", "private_group"
    @SerializedName("is_group") val isGroup: Boolean = false,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("description") val description: String? = null,
    @SerializedName("members_count") val membersCount: Int = 0,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("pinned_message_id") val pinnedMessageId: Long? = null
)

data class Message(
    @SerializedName("id") val id: Long,
    @SerializedName("from_id") val fromId: Long,
    @SerializedName("to_id") val toId: Long,
    @SerializedName("text") val encryptedText: String,
    @SerializedName("time") val timeStamp: Long,
    @SerializedName("media") val mediaUrl: String?,
    @SerializedName("type") val type: String, // "text", "image", "video", "audio", "voice", "file"
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
    val decryptedText: String? = null
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
    @SerializedName("created_time") val createdTime: Long,
    @SerializedName("members") val members: List<GroupMember>? = null,
    @SerializedName("pinned_message_id") val pinnedMessageId: Long? = null
)

data class GroupMember(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("role") val role: String, // "admin", "moderator", "member"
    @SerializedName("joined_time") val joinedTime: Long,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("is_blocked") val isBlocked: Boolean = false
)

data class MediaFile(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: String, // "image", "video", "audio", "voice", "file"
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("size") val size: Long,
    @SerializedName("duration") val duration: Long? = null, // для видео/аудио в миллисекундах
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("created_time") val createdTime: Long
)

data class VoiceMessage(
    @SerializedName("id") val id: String,
    val localPath: String? = null, // для записи перед отправкой
    @SerializedName("url") val url: String? = null,
    @SerializedName("duration") val duration: Long, // миллисекунды
    @SerializedName("size") val size: Long,
    @SerializedName("created_time") val createdTime: Long
)

// ==================== API RESPONSES ====================

data class ChatListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("data") val chats: List<Chat>?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class MessageListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("messages") val messages: List<Message>?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class GroupListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("groups") val groups: List<Group>?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class GroupDetailResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("group") val group: Group?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class MediaUploadResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("media_id") val mediaId: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isPrivate: Boolean = false,
    val memberIds: List<Long> = emptyList()
)

data class CreateGroupResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("group_id") val groupId: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class AuthResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)