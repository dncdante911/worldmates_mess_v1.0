package com.worldmates.messenger.network

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

    // ==================== CHATS ====================

    @FormUrlEncoded
    @POST(Constants.GET_CHATS_ENDPOINT)
    suspend fun getChats(
        @Field("access_token") accessToken: String,
        @Field("user_limit") limit: Int = 50,
        @Field("data_type") dataType: String = "all", // "all", "users", "groups", "channels"
        @Field("SetOnline") setOnline: Int = 1,
        @Field("offset") offset: Int = 0
    ): ChatListResponse

    @FormUrlEncoded
    @POST(Constants.GET_MESSAGES_ENDPOINT)
    suspend fun getMessages(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("limit") limit: Int = 30,
        @Field("before_message_id") beforeMessageId: Long = 0
    ): MessageListResponse

    // ==================== GROUPS ====================

    @FormUrlEncoded
    @POST("?type=get_groups")
    suspend fun getGroups(
        @Field("access_token") accessToken: String,
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0
    ): GroupListResponse

    @FormUrlEncoded
    @POST("?type=get_group_details")
    suspend fun getGroupDetails(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long
    ): GroupDetailResponse

    @FormUrlEncoded
    @POST("?type=create_group")
    suspend fun createGroup(
        @Field("access_token") accessToken: String,
        @Field("name") name: String,
        @Field("description") description: String? = null,
        @Field("is_private") isPrivate: Int = 0,
        @Field("member_ids") memberIds: String = "" // JSON array or comma-separated
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=update_group")
    suspend fun updateGroup(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("name") name: String? = null,
        @Field("description") description: String? = null,
        @Field("avatar") avatarUrl: String? = null
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=delete_group")
    suspend fun deleteGroup(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=add_group_member")
    suspend fun addGroupMember(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("user_id") userId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=remove_group_member")
    suspend fun removeGroupMember(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("user_id") userId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=set_group_admin")
    suspend fun setGroupAdmin(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("user_id") userId: Long,
        @Field("role") role: String = "admin" // "admin", "moderator", "member"
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=leave_group")
    suspend fun leaveGroup(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long
    ): CreateGroupResponse

    @FormUrlEncoded
    @POST("?type=get_group_members")
    suspend fun getGroupMembers(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("limit") limit: Int = 100,
        @Field("offset") offset: Int = 0
    ): GroupDetailResponse

    // ==================== MESSAGES ====================

    @FormUrlEncoded
    @POST("?type=send_message")
    suspend fun sendMessage(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("text") text: String,
        @Field("send_time") sendTime: Long
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=send_group_message")
    suspend fun sendGroupMessage(
        @Field("access_token") accessToken: String,
        @Field("group_id") groupId: Long,
        @Field("text") text: String,
        @Field("send_time") sendTime: Long
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=delete_message")
    suspend fun deleteMessage(
        @Field("access_token") accessToken: String,
        @Field("message_id") messageId: Long
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=edit_message")
    suspend fun editMessage(
        @Field("access_token") accessToken: String,
        @Field("message_id") messageId: Long,
        @Field("text") newText: String
    ): MessageResponse

    // ==================== MEDIA UPLOAD ====================

    @Multipart
    @POST("?type=upload_media")
    suspend fun uploadMedia(
        @Part("access_token") accessToken: RequestBody,
        @Part("media_type") mediaType: RequestBody, // "image", "video", "audio", "voice", "file"
        @Part("recipient_id") recipientId: RequestBody? = null,
        @Part("group_id") groupId: RequestBody? = null,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    @Multipart
    @POST("?type=upload_group_avatar")
    suspend fun uploadGroupAvatar(
        @Part("access_token") accessToken: RequestBody,
        @Part("group_id") groupId: RequestBody,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    @Multipart
    @POST("?type=upload_user_avatar")
    suspend fun uploadUserAvatar(
        @Part("access_token") accessToken: RequestBody,
        @Part file: MultipartBody.Part
    ): MediaUploadResponse

    // ==================== TYPING & PRESENCE ====================

    @FormUrlEncoded
    @POST("?type=set_typing")
    suspend fun setTyping(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("is_typing") isTyping: Int = 1
    ): MessageResponse

    @FormUrlEncoded
    @POST("?type=set_last_seen")
    suspend fun setLastSeen(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long
    ): MessageResponse

    // ==================== VOICE/VIDEO CALLS ====================

    @FormUrlEncoded
    @POST("?type=initiate_call")
    suspend fun initiateCall(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("call_type") callType: String, // "voice", "video"
        @Field("rtc_offer") rtcOffer: String? = null
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=accept_call")
    suspend fun acceptCall(
        @Field("access_token") accessToken: String,
        @Field("call_id") callId: String,
        @Field("rtc_answer") rtcAnswer: String? = null
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=reject_call")
    suspend fun rejectCall(
        @Field("access_token") accessToken: String,
        @Field("call_id") callId: String
    ): CallResponse

    @FormUrlEncoded
    @POST("?type=end_call")
    suspend fun endCall(
        @Field("access_token") accessToken: String,
        @Field("call_id") callId: String,
        @Field("duration") duration: Long
    ): CallResponse
}

// ==================== RESPONSE MODELS ====================

data class MessageResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message_id") val messageId: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class CallResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("call_id") val callId: String?,
    @SerializedName("rtc_signal") val rtcSignal: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)