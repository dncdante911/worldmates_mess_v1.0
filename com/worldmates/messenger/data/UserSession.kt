package com.worldmates.messenger.data

object UserSession {
    var accessToken: String? = null
    var userId: Long = 0
    var isLoggedIn: Boolean = false

    fun saveSession(token: String, id: Long) {
        accessToken = token
        userId = id
        isLoggedIn = true
        // В реальном приложении: сохраните это в SharedPreferences или DataStore.
    }

    fun clearSession() {
        accessToken = null
        userId = 0
        isLoggedIn = false
    }
}