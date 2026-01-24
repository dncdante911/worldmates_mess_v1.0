package com.worldmates.messenger.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.User
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    /**
     * Завантажити дані профілю користувача
     */
    fun loadUserProfile(userId: Long? = null) {
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            try {
                val accessToken = UserSession.accessToken ?: throw Exception("No access token")
                // If userId is null, load current user's profile
                val targetUserId = userId ?: UserSession.userId

                val response = RetrofitClient.apiService.getUserData(
                    accessToken = accessToken,
                    userId = targetUserId
                )

                Log.d("UserProfileViewModel", "Profile response: apiStatus=${response.apiStatus}")

                if (response.apiStatus == 200 && response.userData != null) {
                    _profileState.value = ProfileState.Success(response.userData)
                    Log.d("UserProfileViewModel", "Profile loaded: ${response.userData.username}")
                } else {
                    val errorMsg = response.errorMessage ?: "Failed to load profile"
                    _profileState.value = ProfileState.Error(errorMsg)
                    Log.e("UserProfileViewModel", "Error: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Network error: ${e.localizedMessage}"
                _profileState.value = ProfileState.Error(errorMsg)
                Log.e("UserProfileViewModel", "Exception loading profile", e)
            }
        }
    }

    /**
     * Оновити дані профілю
     */
    fun updateProfile(
        firstName: String?,
        lastName: String?,
        about: String?,
        birthday: String?,
        gender: String?,
        phoneNumber: String?,
        website: String?,
        working: String?,
        address: String?,
        city: String?,
        school: String?
    ) {
        _updateState.value = UpdateState.Loading

        viewModelScope.launch {
            try {
                val accessToken = UserSession.accessToken ?: throw Exception("No access token")

                val response = RetrofitClient.apiService.updateUserData(
                    accessToken = accessToken,
                    firstName = firstName,
                    lastName = lastName,
                    about = about,
                    birthday = birthday,
                    gender = gender,
                    phoneNumber = phoneNumber,
                    website = website,
                    working = working,
                    address = address,
                    city = city,
                    school = school
                )

                Log.d("UserProfileViewModel", "Update response: apiStatus=${response.apiStatus}")

                if (response.apiStatus == 200) {
                    _updateState.value = UpdateState.Success
                    // Reload profile to get updated data
                    loadUserProfile()
                } else {
                    val errorMsg = response.errorMessage ?: "Failed to update profile"
                    _updateState.value = UpdateState.Error(errorMsg)
                    Log.e("UserProfileViewModel", "Update error: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Network error: ${e.localizedMessage}"
                _updateState.value = UpdateState.Error(errorMsg)
                Log.e("UserProfileViewModel", "Exception updating profile", e)
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}
