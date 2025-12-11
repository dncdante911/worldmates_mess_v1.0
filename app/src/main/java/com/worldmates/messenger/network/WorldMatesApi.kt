package com.worldmates.messenger.network

import com.google.gson.annotations.SerializedName
import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface WorldMatesApi {

    // ==================== AUTHENTICATION ====================

    @FormUrlEncoded
    @POST(Constants.AUTH_ENDPOINT)
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("device_type") deviceType: String = "phone",
        @Field("android_m_device_id") deviceId: String? = null
    ): AuthResponse

    @FormUrlEncoded
    @POST("?type=create-account")
    suspend fun register(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("confirm_password") confirmPassword: String,
        @Field("device_type") deviceType: String = "phone",
        @Field("gender") gender: String = "male",
        @Field("android_m_device_id") deviceId: String? = null
    ): AuthResponse

    @FormUrlEncoded
    @POST("/api/v2/sync_session.php")
    suspend fun syncSession(
        @Query("access_token") accessToken: String,
        @Field("user_id") userId: Long,
        @Field("platform") platform: String = "phone"
    ): SyncSessionResponse

    // ==================== CHATS ====================

    @FormUrlEncoded
    @POST(Constants.GET_CHATS_ENDPOINT)
    suspend fun getChats(
        @Query("access_token") accessToken: String,
        @Field("user_limit") limit: Int = 50,
        @Field("data_type") dataType: String = "all", // "all", "users", "groups", "channels"
        @Field("SetOnline") setOnline: Int = 1,
        @Field("offset") offset: Int = 0
    ): ChatListResponse

    @FormUrlEncoded
    @POST("?type=search")
    suspend fun searchUsers(
        @Query("access_token") accessToken: String,
        @Field("query") query: String,
        @Field("limit") limit: Int = 30,
        @Field("offset") offset: Int = 0
    ): UserSearchResponse

    @FormUrlEncoded
    @POST(Constants.GET_MESSAGES_ENDPOINT)
    suspend fun getMessages(
        @Query("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("limit") limit: Int = 30,
        @Field("before_message_id") beforeMessageId: Long = 0
    ): MessageListResponse

    // ==================== GROUP CHATS (Messenger Groups) ====================
    // Uses /api/v2/group_chat_v2.php - NEW custom API endpoint with 'type' parameter

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun getGroups(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_list",
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0
    ): GroupListResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun getGroupDetails(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_by_id",
        @Field("id") groupId: Long
    ): GroupDetailResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun createGroup(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "create",
        @Field("group_name") name: String,
        @Field("parts") memberIds: String = "", // Comma-separated user IDs
        @Field("group_type") groupType: String = "group" // "group" or "channel"
    ): CreateGroupResponse?

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun updateGroup(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "edit",
        @Field("id") groupId: Long,
        @Field("group_name") name: String
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun deleteGroup(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "delete",
        @Field("id") groupId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun addGroupMember(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "add_user",
        @Field("id") groupId: Long,
        @Field("parts") userIds: String // Comma-separated user IDs
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun removeGroupMember(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "remove_user",
        @Field("id") groupId: Long,
        @Field("parts") userIds: String // Comma-separated user IDs
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun setGroupAdmin(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "set_admin",
        @Field("id") groupId: Long,
        @Field("user_id") userId: Long,
        @Field("role") role: String = "admin"
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun leaveGroup(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "leave",
        @Field("id") groupId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun getGroupMembers(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_members",
        @Field("id") groupId: Long,
        @Field("limit") limit: Int = 100,
        @Field("offset") offset: Int = 0
    ): GroupDetailResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun getGroupMessages(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_messages",
        @Field("id") groupId: Long,
        @Field("limit") limit: Int = 50,
        @Field("before_message_id") beforeMessageId: Long = 0
    ): MessageListResponse

    @FormUrlEncoded
    @POST("/api/v2/group_chat_v2.php")
    suspend fun sendGroupMessage(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "send_message",
        @Field("id") groupId: Long,
        @Field("text") text: String
    ): MessageResponse

    @Multipart
    @POST("/api/v2/group_chat_v2.php")
    suspend fun uploadGroupAvatar(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "upload_avatar",
        @Part("id") groupId: RequestBody,
        @Part avatar: MultipartBody.Part
    ): CreateGroupResponse

    // ==================== MESSAGES ====================

    @FormUrlEncoded
    @POST("?type=send-message")
    suspend fun sendMessage(
        @Query("access_token") accessToken: String,
        @Field("user_id") recipientId: Long,
        @Field("text") text: String,
        @Field("message_hash_id") messageHashId: String
    ): MessageResponse

    // Отправка сообщения с медиа-файлом
    @Multipart
    @POST("?type=send-message")
    suspend fun sendMessageWithMedia(
        @Query("access_token") accessToken: String,
        @Part("user_id") recipientId: RequestBody,
        @Part("text") text: RequestBody,
        @Part("message_hash_id") messageHashId: RequestBody,
        @Part file: MultipartBody.Part
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=delete_message")
    suspend fun deleteMessage(
        @Query("access_token") accessToken: String,
        @Field("message_id") messageId: Long
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=edit_message")
    suspend fun editMessage(
        @Query("access_token") accessToken: String,
        @Field("message_id") messageId: Long,
        @Field("text") newText: String
    ): MessageResponse

    // ==================== MEDIA UPLOAD ====================

    // XHR Upload endpoints (используются для загрузки файлов на сервер)
    // Используем абсолютный путь (с /) чтобы обойти api/v2/
    // Параметр f= указывает какой обработчик использовать
    @Multipart
    @POST("/xhr/upload_image.php")
    suspend fun uploadImage(
        @Query("access_token") accessToken: String,
        @Query("f") f: String = "upload_image",
        @Part image: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_video.php")
    suspend fun uploadVideo(
        @Query("access_token") accessToken: String,
        @Query("f") f: String = "upload_video",
        @Part video: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_audio.php")
    suspend fun uploadAudio(
        @Query("access_token") accessToken: String,
        @Query("f") f: String = "upload_audio",
        @Part audio: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_file.php")
    suspend fun uploadFile(
        @Query("access_token") accessToken: String,
        @Query("f") f: String = "upload_file",
        @Part file: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("?type=upload_media")
    suspend fun uploadMedia(
        @Query("access_token") accessToken: String,
        @Part("media_type") mediaType: RequestBody, // "image", "video", "audio", "voice", "file"
        @Part("recipient_id") recipientId: RequestBody? = null,
        @Part("group_id") groupId: RequestBody? = null,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    @Multipart
    @POST("?type=upload_group_avatar")
    suspend fun uploadGroupAvatar(
        @Query("access_token") accessToken: String,
        @Part("group_id") groupId: RequestBody,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    @Multipart
    @POST("?type=upload_user_avatar")
    suspend fun uploadUserAvatar(
        @Query("access_token") accessToken: String,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    // ==================== TYPING & PRESENCE ====================

    @FormUrlEncoded
    @POST("?type=set_typing")
    suspend fun setTyping(
        @Query("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("is_typing") isTyping: Int = 1
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=set_last_seen")
    suspend fun setLastSeen(
        @Query("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long
    ): MessageResponse

    // ==================== VOICE/VIDEO CALLS ====================

    @FormUrlEncoded
    @POST("?type=initiate_call")
    suspend fun initiateCall(
        @Query("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("call_type") callType: String, // "voice", "video"
        @Field("rtc_offer") rtcOffer: String? = null
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=accept_call")
    suspend fun acceptCall(
        @Query("access_token") accessToken: String,
        @Field("call_id") callId: String,
        @Field("rtc_answer") rtcAnswer: String? = null
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=reject_call")
    suspend fun rejectCall(
        @Query("access_token") accessToken: String,
        @Field("call_id") callId: String
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=end_call")
    suspend fun endCall(
        @Query("access_token") accessToken: String,
        @Field("call_id") callId: String,
        @Field("duration") duration: Long
    ): CallResponse
}

// ==================== RESPONSE MODELS ====================

data class MessageResponse(
    @SerializedName("api_status") val apiStatusString: String?, // "200" или "400"
    @SerializedName("api_text") val apiText: String?, // "success" или "failed"
    @SerializedName("api_version") val apiVersion: String?,
    @SerializedName("messages") val messages: List<Message>?,
    @SerializedName("message_data") val messageData: List<Message>?, // API иногда возвращает message_data
    @SerializedName("message_id") val messageId: Long?,
    @SerializedName("errors") val errors: ErrorDetails?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
) {
    // Для совместимости с кодом, проверяющим apiStatus как Int
    val apiStatus: Int
        get() = apiStatusString?.toIntOrNull() ?: 400

    // Универсальный геттер для получения сообщений (из messages или message_data)
    val allMessages: List<Message>?
        get() = messages ?: messageData
}

data class ErrorDetails(
    @SerializedName("error_id") val errorId: String?,
    @SerializedName("error_text") val errorText: String?
)

data class CallResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("call_id") val callId: String?,
    @SerializedName("rtc_signal") val rtcSignal: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class SearchUser(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String?,
    @SerializedName("avatar") val avatarUrl: String,
    @SerializedName("verified") val verified: Int = 0,
    @SerializedName("lastseen") val lastSeen: Long?,
    @SerializedName("lastseen_status") val lastSeenStatus: String?, // "online", "offline", "recently"
    @SerializedName("about") val about: String?
)

data class UserSearchResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("users") val users: List<SearchUser>?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

/**
 * Response from XHR upload endpoints (upload_image.php, upload_video.php, etc.)
 */
data class XhrUploadResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("image") val imageUrl: String?, // URL для изображений
    @SerializedName("image_src") val imageSrc: String?, // Путь к файлу
    @SerializedName("video") val videoUrl: String?, // URL для видео
    @SerializedName("video_src") val videoSrc: String?, // Путь к видео
    @SerializedName("audio") val audioUrl: String?, // URL для аудио
    @SerializedName("audio_src") val audioSrc: String?, // Путь к аудио
    @SerializedName("file") val fileUrl: String?, // URL для файлов
    @SerializedName("file_src") val fileSrc: String?, // Путь к файлу
    @SerializedName("error") val error: String?
)