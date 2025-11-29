package com.worldmates.messenger.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.RetrofitClient 
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // Вызываем WorldMatesApi через RetrofitClient
                val response = RetrofitClient.apiService.login(username, password) 

                if (response.apiStatus == 200 && response.accessToken != null && response.userId != null) {
                    UserSession.saveSession(response.accessToken, response.userId)
                    _loginState.value = LoginState.Success
                } else {
                    // Обработка ошибок из auth.php
                    val errorMsg = response.errorMessage ?: "Ошибка входа. Проверьте учетные данные."
                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Ошибка сети: ${e.message}")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}