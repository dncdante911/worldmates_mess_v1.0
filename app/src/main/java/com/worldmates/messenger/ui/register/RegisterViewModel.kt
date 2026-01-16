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
                // Генеруємо унікальний session ID для WoWonder API
                val sessionId = generateSessionId()

                // Виклик API для регистрации
                val response = RetrofitClient.apiService.register(
                    username = username,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    sessionId = sessionId,
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

    /**
     * Регистрация с верификацией через Email
     */
    fun registerWithEmail(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
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
                val response = RetrofitClient.apiService.registerWithVerification(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    verificationType = "email",
                    email = email,
                    phoneNumber = null
                )

                if (response.apiStatus == 200) {
                    _registerState.value = RegisterState.VerificationRequired(
                        userId = response.userId ?: 0,
                        username = response.username ?: username,
                        verificationType = "email",
                        contactInfo = email
                    )
                    Log.d("RegisterViewModel", "Реєстрація успішна, потрібна верифікація")
                } else {
                    val errorMsg = response.errors ?: response.message ?: "Помилка реєстрації"
                    _registerState.value = RegisterState.Error(errorMsg)
                    Log.e("RegisterViewModel", "Помилка: $errorMsg")
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
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Помилка реєстрації", e)
            }
        }
    }

    /**
     * Регистрация с верификацией через SMS
     */
    fun registerWithPhone(
        username: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ) {
        if (username.isBlank() || phoneNumber.isBlank() || password.isBlank()) {
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
                val response = RetrofitClient.apiService.registerWithVerification(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    verificationType = "phone",
                    email = null,
                    phoneNumber = phoneNumber
                )

                if (response.apiStatus == 200) {
                    _registerState.value = RegisterState.VerificationRequired(
                        userId = response.userId ?: 0,
                        username = response.username ?: username,
                        verificationType = "phone",
                        contactInfo = phoneNumber
                    )
                    Log.d("RegisterViewModel", "Реєстрація успішна, потрібна верифікація")
                } else {
                    val errorMsg = response.errors ?: response.message ?: "Помилка реєстрації"
                    _registerState.value = RegisterState.Error(errorMsg)
                    Log.e("RegisterViewModel", "Помилка: $errorMsg")
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
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Помилка реєстрації", e)
            }
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }

    /**
     * Генерує унікальний session ID для WoWonder API
     * Формат схожий на те, що генерує сервер: SHA1 + MD5 + random
     */
    private fun generateSessionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (100000000..999999999).random()
        val baseString = "$timestamp-$random-${System.nanoTime()}"

        // Генеруємо хеш схожий на WoWonder формат
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(baseString.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class VerificationRequired(
        val userId: Long,
        val username: String,
        val verificationType: String,
        val contactInfo: String
    ) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
