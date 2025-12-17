# 🔧 ГОТОВЫЕ ИСПРАВЛЕНИЯ ДЛЯ КОПИРОВАНИЯ

Эти исправления нужно применить к Android коду для полной интеграции SMS/Email верификации.

---

## 1. WorldMatesApi.kt - Обновить методы верификации

**Файл:** `app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt`

**Заменить строки 35-50 на:**

```kotlin
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
```

**Добавить в конец файла (после строки 439):**

```kotlin

// ==================== VERIFICATION RESPONSE MODELS ====================

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

data class VerifyCodeResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("errors") val errors: String? = null
)

data class ResendCodeResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("code_length") val codeLength: Int? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("errors") val errors: String? = null
)
```

---

## 2. RegisterViewModel.kt - Добавить методы верификации

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/register/RegisterViewModel.kt`

**Заменить sealed class (строки 92-97) на:**

```kotlin
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
```

**Добавить перед `resetState()` (после строки 85):**

```kotlin

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
            } catch (e: Exception) {
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _registerState.value = RegisterState.Error(errorMsg)
                Log.e("RegisterViewModel", "Помилка реєстрації", e)
            }
        }
    }
```

---

## 3. VerificationViewModel.kt - Обновить методы

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/verification/VerificationViewModel.kt`

**Заменить метод `sendVerificationCode` (строки 24-54) на:**

```kotlin
    /**
     * Отправка кода верификации
     */
    fun sendVerificationCode(
        verificationType: String,
        contactInfo: String,
        username: String? = null
    ) {
        if (_resendTimer.value > 0) {
            Log.d("VerificationVM", "Таймер ще не закінчився: ${_resendTimer.value}")
            return
        }

        _verificationState.value = VerificationState.Sending

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.sendVerificationCode(
                    verificationType = verificationType,
                    contactInfo = contactInfo,
                    username = username
                )

                if (response.actualStatus == 200) {
                    _verificationState.value = VerificationState.CodeSent
                    Log.d("VerificationVM", "Код успішно надіслано на $contactInfo")
                    startResendTimer()
                } else {
                    val errorMsg = response.errors ?: response.message ?: "Помилка відправки коду"
                    _verificationState.value = VerificationState.Error(errorMsg)
                    Log.e("VerificationVM", "Помилка: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _verificationState.value = VerificationState.Error(errorMsg)
                Log.e("VerificationVM", "Помилка відправки коду", e)
            }
        }
    }
```

**Заменить метод `verifyCode` (строки 59-104) на:**

```kotlin
    /**
     * Проверка кода верификации
     */
    fun verifyCode(
        verificationType: String,
        contactInfo: String,
        code: String,
        username: String? = null
    ) {
        if (code.length != 6) {
            _verificationState.value = VerificationState.Error("Код має містити 6 цифр")
            return
        }

        _verificationState.value = VerificationState.Loading

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.verifyCode(
                    verificationType = verificationType,
                    contactInfo = contactInfo,
                    code = code,
                    username = username
                )

                when {
                    response.apiStatus == 200 && response.accessToken != null && response.userId != null -> {
                        // Успешная верификация - сохраняем сессию
                        UserSession.saveSession(
                            response.accessToken,
                            response.userId,
                            username,
                            null
                        )
                        _verificationState.value = VerificationState.Success
                        Log.d("VerificationVM", "Верифікацію успішно завершено! User ID: ${response.userId}")
                    }
                    response.apiStatus == 400 -> {
                        val errorMsg = response.errors ?: response.message ?: "Невірний код"
                        _verificationState.value = VerificationState.Error(errorMsg)
                        Log.e("VerificationVM", "Помилка верифікації: $errorMsg")
                    }
                    else -> {
                        val errorMsg = response.errors ?: response.message ?: "Невідома помилка"
                        _verificationState.value = VerificationState.Error(errorMsg)
                        Log.e("VerificationVM", "Помилка: ${response.apiStatus} - $errorMsg")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _verificationState.value = VerificationState.Error(errorMsg)
                Log.e("VerificationVM", "Помилка верифікації", e)
            }
        }
    }
```

**Добавить после метода `verifyCode` (после строки 104):**

```kotlin

    /**
     * Повторная отправка кода
     */
    fun resendCode(
        verificationType: String,
        contactInfo: String,
        username: String? = null
    ) {
        if (_resendTimer.value > 0) {
            Log.d("VerificationVM", "Таймер ще не закінчився: ${_resendTimer.value}")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.resendVerificationCode(
                    verificationType = verificationType,
                    contactInfo = contactInfo,
                    username = username
                )

                if (response.apiStatus == 200) {
                    _verificationState.value = VerificationState.CodeSent
                    Log.d("VerificationVM", "Код успішно надіслано повторно")
                    startResendTimer()
                } else {
                    val errorMsg = response.errors ?: response.message ?: "Помилка відправки коду"
                    _verificationState.value = VerificationState.Error(errorMsg)
                    Log.e("VerificationVM", "Помилка: $errorMsg")
                }
            } catch (e: Exception) {
                val errorMsg = "Помилка мережі: ${e.localizedMessage}"
                _verificationState.value = VerificationState.Error(errorMsg)
                Log.e("VerificationVM", "Помилка повторної відправки коду", e)
            }
        }
    }
```

---

## 4. Использование в RegisterActivity

**Обновить логику в RegisterActivity.kt для перехода на верификацию:**

```kotlin
lifecycleScope.launch {
    viewModel.registerState.collect { state ->
        when (state) {
            is RegisterState.Success -> {
                // Старый flow - прямой логин без верификации
                navigateToChats()
            }
            is RegisterState.VerificationRequired -> {
                // Новый flow - переход на экран верификации
                val intent = Intent(this@RegisterActivity, VerificationActivity::class.java).apply {
                    putExtra("verification_type", state.verificationType)
                    putExtra("contact_info", state.contactInfo)
                    putExtra("username", state.username)
                    putExtra("is_registration", true)
                }
                startActivity(intent)
                finish()
            }
            is RegisterState.Error -> {
                Toast.makeText(
                    this@RegisterActivity,
                    state.message,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {}
        }
    }
}
```

---

## 5. SQL Миграции для БД

**Выполнить на сервере:**

```sql
-- 1. Создать таблицу для rate limiting
CREATE TABLE IF NOT EXISTS `rate_limits` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `action` VARCHAR(50) NOT NULL,
    `identifier` VARCHAR(100) NOT NULL,
    `timestamp` INT NOT NULL,
    INDEX `idx_action_identifier` (`action`, `identifier`),
    INDEX `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Добавить поле для времени истечения кода
ALTER TABLE `Wo_Users`
ADD COLUMN IF NOT EXISTS `verification_code_expires` INT(11) NOT NULL DEFAULT 0 AFTER `sms_code`;

-- 3. Добавить индекс для активации
ALTER TABLE `Wo_Users`
ADD INDEX IF NOT EXISTS `idx_active` (`active`);

-- 4. Создать таблицу логов верификации
CREATE TABLE IF NOT EXISTS `verification_logs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT(11) NOT NULL,
    `verification_type` ENUM('email', 'phone') NOT NULL,
    `contact_info` VARCHAR(255) NOT NULL,
    `code_sent` TINYINT(1) NOT NULL DEFAULT 1,
    `code_verified` TINYINT(1) NOT NULL DEFAULT 0,
    `ip_address` VARCHAR(100) DEFAULT '',
    `user_agent` VARCHAR(500) DEFAULT '',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6. Тестирование

**После применения исправлений протестируйте:**

```bash
# 1. Email Registration
curl -X POST 'https://your-domain.com/xhr/index.php?f=register_with_verification' \
  -d 'username=testuser' \
  -d 'password=test123' \
  -d 'confirm_password=test123' \
  -d 'verification_type=email' \
  -d 'email=test@example.com'

# 2. Verify Code
curl -X POST 'https://your-domain.com/xhr/index.php?f=verify_code' \
  -d 'verification_type=email' \
  -d 'contact_info=test@example.com' \
  -d 'code=123456' \
  -d 'username=testuser'

# 3. Resend Code
curl -X POST 'https://your-domain.com/xhr/index.php?f=resend_verification_code' \
  -d 'verification_type=email' \
  -d 'contact_info=test@example.com' \
  -d 'username=testuser'
```

---

**Готово! После применения всех исправлений система верификации будет полностью рабочей** ✅
