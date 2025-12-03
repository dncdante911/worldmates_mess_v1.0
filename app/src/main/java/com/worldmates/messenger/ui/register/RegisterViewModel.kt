package com.worldmates.messenger.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _registerState.value = RegisterState.Error("Заповніть всі поля")
            return
        }

        if (password != confirmPassword) {
            _registerState.value = RegisterState.Error("Паролі не співпадають")
            return
        }

        if (password.length < 6) {
            _registerState.value = RegisterState.Error("Пароль має містити мінімум 6 символів")
            return
        }

        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                // Виклик API для регистрации
                val response = RetrofitClient.apiService.register(
                    username = username,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    deviceType = "phone"
                )

                // Перевірка статусу відповіді
                when {
                    response.apiStatus == 200 && response.accessToken != null && response.userId != null -> {
                        // Успішна регистрация
                        UserSession.saveSession(
                            response.accessToken,
                            response.userId,
                            response.username,
                            response.avatar
                        )
                        _registerState.value = RegisterState.Success
                        Log.d("RegisterViewModel", "Успішно зареєстровано! User ID: ${response.userId}")
                    }
                    response.apiStatus == 400 -> {
                        // Помилка від сервера
                        val errorMsg = response.errorMessage ?: "Помилка реєстрації"
                        _registerState.value = RegisterState.Error(errorMsg)
                        Log.e("RegisterViewModel", "Помилка реєстрації: $errorMsg")
                    }
                    else -> {
                        val errorMsg = response.errorMessage ?: "Невідома помилка"
                        _registerState.value = RegisterState.Error(errorMsg)
                        Log.e("RegisterViewModel", "Помилка: ${response.apiStatus} - $errorMsg")
                    }
                }
            } catch (e: java.net.ConnectException) {
                val errorMsg = "Помилка з'єднання. Перевірте мережу"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Помилка з'єднання", e)
            } catch (e: java.net.SocketTimeoutException) {
                val errorMsg = "Тайм-аут з'єднання. Спробуйте ще раз"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Тайм-аут", e)
            } catch (e: Exception) {
                val errorMsg = "Помилка мережи: ${e.localizedMessage}"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Помилка реєстрації", e)
            }
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}
