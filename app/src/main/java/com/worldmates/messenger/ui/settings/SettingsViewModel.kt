package com.worldmates.messenger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _username = MutableStateFlow(UserSession.username ?: "")
    val username: StateFlow<String> = _username

    private val _avatar = MutableStateFlow(UserSession.avatar)
    val avatar: StateFlow<String?> = _avatar

    private val _userId = MutableStateFlow(UserSession.userId)
    val userId: StateFlow<Long> = _userId

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _username.value = UserSession.username ?: ""
            _avatar.value = UserSession.avatar
            _userId.value = UserSession.userId
        }
    }

    fun updateUsername(newUsername: String) {
        _username.value = newUsername
        UserSession.username = newUsername
    }

    fun updateAvatar(newAvatar: String) {
        _avatar.value = newAvatar
        UserSession.avatar = newAvatar
    }
}
