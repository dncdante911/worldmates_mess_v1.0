package com.worldmates.messenger.data.model

import com.google.gson.annotations.SerializedName

data class Chat(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("last_message") val lastMessage: Message?,
    @SerializedName("message_count") val unreadCount: Int,
    @SerializedName("chat_type") val chatType: String // "user" или "group"
)

data class Message(
    @SerializedName("id") val id: Long,
    @SerializedName("from_id") val fromId: Long,
    @SerializedName("to_id") val toId: Long,
    @SerializedName("text") val encryptedText: String, // Зашифрованный текст
    @SerializedName("time") val timeStamp: Long, // Используется как ключ для дешифрования
    @SerializedName("media") val mediaUrl: String?,
    @SerializedName("type") val type: String, // например, "right_text"
    val decryptedText: String? = null // Для хранения дешифрованного текста в приложении
)

data class ChatListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("data") val chats: List<Chat>?
)

data class MessageListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("messages") val messages: List<Message>?
)