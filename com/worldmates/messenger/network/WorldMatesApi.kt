package com.worldmates.messenger.network

import com.worldmates.messenger.data.Constants
import com.worldmates.messenger.data.model.AuthResponse
import com.worldmates.messenger.data.model.ChatListResponse
import com.worldmates.messenger.data.model.MessageListResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface WorldMatesApi { // <--- ИЗМЕНЕНО

    /**
     * API для аутентификации пользователя (логин).
     * Соответствует логике в файле auth.php.
     */
    @FormUrlEncoded
    @POST(Constants.AUTH_ENDPOINT)
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("device_type") deviceType: String = "phone",
        @Field("android_m_device_id") deviceId: String? = null // Для FCM
    ): AuthResponse

    /**
     * API для получения списка чатов.
     * Соответствует логике в файле get_chats.php.
     */
    @FormUrlEncoded
    @POST(Constants.GET_CHATS_ENDPOINT)
    suspend fun getChats(
        @Field("access_token") accessToken: String,
        @Field("user_limit") limit: Int = 50,
        @Field("data_type") dataType: String = "all", // "all", "users", "groups"
        @Field("SetOnline") setOnline: Int = 1 // Обновляем lastseen
    ): ChatListResponse

    /**
     * API для получения истории сообщений в чате.
     * Соответствует логике в файле get_user_messages.php.
     */
    @FormUrlEncoded
    @POST(Constants.GET_MESSAGES_ENDPOINT)
    suspend fun getMessages(
        @Field("access_token") accessToken: String,
        @Field("recipient_id") recipientId: Long,
        @Field("limit") limit: Int = 30,
        @Field("before_message_id") beforeMessageId: Long = 0 // Для подгрузки старых сообщений
    ): MessageListResponse
}