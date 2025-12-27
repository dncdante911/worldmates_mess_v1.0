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

    // ==================== VERIFICATION ====================

    @FormUrlEncoded
    @POST("/xhr/index.php?f=register_with_verification")
    suspend fun registerWithVerification(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("confirm_password") confirmPassword: String,
        @Field("verification_type") verificationType: String, // "email" or "phone"
        @Field("email") email: String? = null,
        @Field("phone_number") phoneNumber: String? = null,
        @Field("gender") gender: String = "male"
    ): RegisterVerificationResponse

    @FormUrlEncoded
    @POST("/xhr/index.php?f=send_verification_code")
    suspend fun sendVerificationCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("username") username: String? = null,
        @Field("user_id") userId: Long? = null
    ): SendCodeResponse

    @FormUrlEncoded
    @POST("/xhr/index.php?f=verify_code")
    suspend fun verifyCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("code") code: String,
        @Field("username") username: String? = null,
        @Field("user_id") userId: Long? = null
    ): VerifyCodeResponse

    @FormUrlEncoded
    @POST("/xhr/index.php?f=resend_verification_code")
    suspend fun resendVerificationCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("username") username: String? = null,
        @Field("user_id") userId: Long? = null
    ): ResendCodeResponse

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
        @Field("text") text: String,
        @Field("message_reply_id") replyToId: Long? = null
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
        @Field("message_hash_id") messageHashId: String,
        @Field("message_reply_id") replyToId: Long? = null
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

    // ==================== REACTIONS ====================

    @FormUrlEncoded
    @POST("?type=add_message_reaction")
    suspend fun addReaction(
        @Query("access_token") accessToken: String,
        @Field("message_id") messageId: Long,
        @Field("reaction") reaction: String
    ): ReactionResponse

    @FormUrlEncoded
    @POST("?type=remove_message_reaction")
    suspend fun removeReaction(
        @Query("access_token") accessToken: String,
        @Field("message_id") messageId: Long,
        @Field("reaction") reaction: String
    ): ReactionResponse

    @GET("?type=get_message_reactions")
    suspend fun getReactions(
        @Query("access_token") accessToken: String,
        @Query("message_id") messageId: Long
    ): ReactionsListResponse

    // ==================== CUSTOM EMOJIS ====================

    @GET("?type=get_emoji_packs")
    suspend fun getEmojiPacks(
        @Query("access_token") accessToken: String
    ): EmojiPacksResponse

    @GET("?type=get_emoji_pack")
    suspend fun getEmojiPack(
        @Query("access_token") accessToken: String,
        @Query("pack_id") packId: Long
    ): EmojiPackDetailResponse

    @FormUrlEncoded
    @POST("?type=activate_emoji_pack")
    suspend fun activateEmojiPack(
        @Query("access_token") accessToken: String,
        @Field("pack_id") packId: Long
    ): EmojiPackDetailResponse

    @FormUrlEncoded
    @POST("?type=deactivate_emoji_pack")
    suspend fun deactivateEmojiPack(
        @Query("access_token") accessToken: String,
        @Field("pack_id") packId: Long
    ): EmojiPackDetailResponse

    // ==================== STICKERS ====================

    @GET("?type=get_sticker_packs")
    suspend fun getStickerPacks(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String
    ): StickerPacksResponse

    @GET("?type=get_sticker_pack")
    suspend fun getStickerPack(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String,
        @Query("pack_id") packId: Long
    ): StickerPackDetailResponse

    @FormUrlEncoded
    @POST("?type=activate_sticker_pack")
    suspend fun activateStickerPack(
        @Query("access_token") accessToken: String,
        @Field("pack_id") packId: Long
    ): StickerPackDetailResponse

    @FormUrlEncoded
    @POST("?type=deactivate_sticker_pack")
    suspend fun deactivateStickerPack(
        @Query("access_token") accessToken: String,
        @Field("pack_id") packId: Long
    ): StickerPackDetailResponse

    @FormUrlEncoded
    @POST("?type=send_sticker")
    suspend fun sendSticker(
        @Query("access_token") accessToken: String,
        @Field("user_id") recipientId: Long? = null,
        @Field("group_id") groupId: Long? = null,
        @Field("sticker_id") stickerId: Long,
        @Field("message_hash_id") messageHashId: String
    ): MessageResponse

    // ==================== MEDIA UPLOAD ====================

    // XHR Upload endpoints (используются для загрузки файлов на сервер)
    // Используем абсолютный путь (с /) чтобы обойти api/v2/
    // Параметр f= указывает какой обработчик использовать
    @Multipart
    @POST("/xhr/upload_image.php")
    suspend fun uploadImage(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String,
        @Query("f") f: String = "upload_image",
        @Part image: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_video.php")
    suspend fun uploadVideo(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String,
        @Query("f") f: String = "upload_video",
        @Part video: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_audio.php")
    suspend fun uploadAudio(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String,
        @Query("f") f: String = "upload_audio",
        @Part audio: MultipartBody.Part
    ): XhrUploadResponse

    @Multipart
    @POST("/xhr/upload_file.php")
    suspend fun uploadFile(
        @Query("access_token") accessToken: String,
        @Query("server_key") serverKey: String,
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

    // ==================== USER PROFILE & SETTINGS ====================

    @FormUrlEncoded
    @POST("?type=get-user-data")
    suspend fun getUserData(
        @Query("access_token") accessToken: String,
        @Field("user_id") userId: Long? = null, // Если null, вернет данные текущего пользователя
        @Field("fetch") fetch: String = "user_data" // Возможные значения: user_data, followers, following, liked_pages, joined_groups, family (через запятую)
    ): GetUserDataResponse

    @FormUrlEncoded
    @POST("?type=update-user-data")
    suspend fun updateUserData(
        @Query("access_token") accessToken: String,
        @Field("first_name") firstName: String? = null,
        @Field("last_name") lastName: String? = null,
        @Field("about") about: String? = null,
        @Field("birthday") birthday: String? = null,
        @Field("gender") gender: String? = null,
        @Field("phone_number") phoneNumber: String? = null,
        @Field("website") website: String? = null,
        @Field("working") working: String? = null,
        @Field("address") address: String? = null,
        @Field("country_id") countryId: String? = null,
        @Field("city") city: String? = null,
        @Field("school") school: String? = null,
        @Field("language") language: String? = null
    ): UpdateUserDataResponse

    @FormUrlEncoded
    @POST("?type=update-privacy-settings")
    suspend fun updatePrivacySettings(
        @Query("access_token") accessToken: String,
        @Field("follow_privacy") followPrivacy: String? = null,
        @Field("friend_privacy") friendPrivacy: String? = null,
        @Field("post_privacy") postPrivacy: String? = null,
        @Field("message_privacy") messagePrivacy: String? = null,
        @Field("confirm_followers") confirmFollowers: String? = null,
        @Field("show_activities_privacy") showActivitiesPrivacy: String? = null,
        @Field("birth_privacy") birthPrivacy: String? = null,
        @Field("visit_privacy") visitPrivacy: String? = null
    ): UpdateUserDataResponse

    @FormUrlEncoded
    @POST("?type=update-notification-settings")
    suspend fun updateNotificationSettings(
        @Query("access_token") accessToken: String,
        @Field("email_notification") emailNotification: Int? = null,
        @Field("e_liked") eLiked: Int? = null,
        @Field("e_wondered") eWondered: Int? = null,
        @Field("e_shared") eShared: Int? = null,
        @Field("e_followed") eFollowed: Int? = null,
        @Field("e_commented") eCommented: Int? = null,
        @Field("e_visited") eVisited: Int? = null,
        @Field("e_liked_page") eLikedPage: Int? = null,
        @Field("e_mentioned") eMentioned: Int? = null,
        @Field("e_joined_group") eJoinedGroup: Int? = null,
        @Field("e_accepted") eAccepted: Int? = null,
        @Field("e_profile_wall_post") eProfileWallPost: Int? = null
    ): UpdateUserDataResponse

    @Multipart
    @POST("?type=update-profile-picture")
    suspend fun updateProfilePicture(
        @Query("access_token") accessToken: String,
        @Part avatar: MultipartBody.Part
    ): MediaUploadResponse

    @Multipart
    @POST("?type=update-cover-picture")
    suspend fun updateCoverPicture(
        @Query("access_token") accessToken: String,
        @Part cover: MultipartBody.Part
    ): MediaUploadResponse

    @FormUrlEncoded
    @POST("?type=get-my-groups")
    suspend fun getMyGroups(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "joined_groups", // Возможные значения: my_groups, joined_groups, category
        @Field("user_id") userId: Long? = null, // Требуется для joined_groups
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0
    ): GroupListResponse

    // ==================== CHANNELS ====================
    // Uses /api/v2/channels.php - Channel-specific API endpoint

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannels(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_list", // "get_list", "get_subscribed", "search"
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0,
        @Field("query") query: String? = null
    ): ChannelListResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannelDetails(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_by_id",
        @Field("channel_id") channelId: Long
    ): ChannelDetailResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun createChannel(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "create_channel",
        @Field("name") name: String,
        @Field("username") username: String? = null,
        @Field("description") description: String? = null,
        @Field("avatar_url") avatarUrl: String? = null,
        @Field("is_private") isPrivate: Int = 0,
        @Field("category") category: String? = null
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun updateChannel(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "update_channel",
        @Field("channel_id") channelId: Long,
        @Field("name") name: String? = null,
        @Field("description") description: String? = null,
        @Field("username") username: String? = null,
        @Field("category") category: String? = null
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun deleteChannel(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "delete_channel",
        @Field("channel_id") channelId: Long
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun subscribeChannel(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "subscribe_channel",
        @Field("channel_id") channelId: Long
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun unsubscribeChannel(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "unsubscribe_channel",
        @Field("channel_id") channelId: Long
    ): CreateChannelResponse

    // Channel Posts
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannelPosts(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "get_channel_posts",
        @Field("channel_id") channelId: Long,
        @Field("limit") limit: Int = 20,
        @Field("before_post_id") beforePostId: Long? = null
    ): ChannelPostsResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun createChannelPost(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "create_post",
        @Field("channel_id") channelId: Long,
        @Field("text") text: String,
        @Field("media_urls") mediaUrls: String? = null, // JSON array of media
        @Field("disable_comments") disableComments: Int = 0,
        @Field("notify_subscribers") notifySubscribers: Int = 1
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun updateChannelPost(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "update_post",
        @Field("post_id") postId: Long,
        @Field("text") text: String,
        @Field("media_urls") mediaUrls: String? = null
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun deleteChannelPost(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "delete_post",
        @Field("post_id") postId: Long
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun pinChannelPost(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "pin_post",
        @Field("post_id") postId: Long
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun unpinChannelPost(
        @Query("access_token") accessToken: String,
        @Field("action") action: String = "unpin_post",
        @Field("post_id") postId: Long
    ): CreatePostResponse

    // Channel Comments
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannelComments(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_comments",
        @Field("post_id") postId: Long,
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0
    ): ChannelCommentsResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun addChannelComment(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "add_comment",
        @Field("post_id") postId: Long,
        @Field("text") text: String,
        @Field("reply_to_id") replyToId: Long? = null
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun deleteChannelComment(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "delete_comment",
        @Field("comment_id") commentId: Long
    ): CreatePostResponse

    // Channel Reactions
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun addPostReaction(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "add_post_reaction",
        @Field("post_id") postId: Long,
        @Field("emoji") emoji: String
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun removePostReaction(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "remove_post_reaction",
        @Field("post_id") postId: Long,
        @Field("emoji") emoji: String
    ): CreatePostResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun addCommentReaction(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "add_comment_reaction",
        @Field("comment_id") commentId: Long,
        @Field("emoji") emoji: String
    ): CreatePostResponse

    // Channel Admin Management
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun addChannelAdmin(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "add_admin",
        @Field("channel_id") channelId: Long,
        @Field("user_id") userId: Long,
        @Field("role") role: String = "moderator" // "admin", "moderator"
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun removeChannelAdmin(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "remove_admin",
        @Field("channel_id") channelId: Long,
        @Field("user_id") userId: Long
    ): CreateChannelResponse

    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun updateChannelSettings(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "update_settings",
        @Field("channel_id") channelId: Long,
        @Field("settings_json") settingsJson: String // JSON string of ChannelSettings
    ): CreateChannelResponse

    // Channel Statistics
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannelStatistics(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_channel_statistics",
        @Field("channel_id") channelId: Long
    ): ChannelStatisticsResponse

    // Channel Subscribers
    @FormUrlEncoded
    @POST("/api/v2/channels.php")
    suspend fun getChannelSubscribers(
        @Query("access_token") accessToken: String,
        @Field("type") type: String = "get_channel_subscribers",
        @Field("channel_id") channelId: Long,
        @Field("limit") limit: Int = 100,
        @Field("offset") offset: Int = 0
    ): ChannelSubscribersResponse

    @Multipart
    @POST("/api/v2/channels.php")
    suspend fun uploadChannelAvatar(
        @Query("access_token") accessToken: String,
        @Query("type") type: String = "upload_avatar",
        @Part("id") channelId: RequestBody,
        @Part avatar: MultipartBody.Part
    ): CreateChannelResponse
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

// ==================== REACTION RESPONSES ====================

data class ReactionResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("api_text") val apiText: String?,
    @SerializedName("reaction") val reaction: MessageReaction?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)

data class ReactionsListResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("api_text") val apiText: String?,
    @SerializedName("reactions") val reactions: List<MessageReaction>?,
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

/**
 * Response для завантаження медіа (аватарки, обкладинки тощо)
 */
data class MediaUploadResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("url") val url: String?,  // URL завантаженого файлу
    @SerializedName("media_id") val mediaId: String?,  // ID медіа (опціонально)
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: ErrorDetails?
) {
    val errorMessage: String?
        get() = errors?.errorText ?: message
}

// ==================== VERIFICATION RESPONSE MODELS ====================

/**
 * Response for registration with verification
 */
data class RegisterVerificationResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("verification_type") val verificationType: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("code_length") val codeLength: Int? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("errors") val errors: String? = null
)

/**
 * Response for sending verification code
 */
data class SendCodeResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("api_status") val apiStatus: Int? = null,
    @SerializedName("message") val message: String?,
    @SerializedName("code_length") val codeLength: Int? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("errors") val errors: String? = null
) {
    val actualStatus: Int
        get() = apiStatus ?: status ?: 400
}

/**
 * Response for verifying code
 */
data class VerifyCodeResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("errors") val errors: String? = null
)

/**
 * Response for resending verification code
 */
data class ResendCodeResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("code_length") val codeLength: Int? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("errors") val errors: String? = null
)

/**
 * Legacy response for verification (deprecated, use specific responses above)
 */
data class VerificationResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)