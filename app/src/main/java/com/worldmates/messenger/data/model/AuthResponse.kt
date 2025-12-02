package com.worldmates.messenger.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("error_message") val errorMessage: String?
)