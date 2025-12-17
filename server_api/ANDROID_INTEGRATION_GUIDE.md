# Android Integration Guide - Verification API

## Overview
This guide shows how to integrate the WorldMates Messenger verification API with your Android application.

---

## API Interface (Retrofit)

Add these methods to your `WorldMatesApi.kt`:

```kotlin
interface WorldMatesApi {

    // 1. Register with verification
    @FormUrlEncoded
    @POST("xhr/index.php?f=register_with_verification")
    suspend fun registerWithVerification(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("confirm_password") confirmPassword: String,
        @Field("verification_type") verificationType: String, // "email" or "phone"
        @Field("email") email: String? = null,
        @Field("phone_number") phoneNumber: String? = null,
        @Field("gender") gender: String = "male"
    ): RegisterVerificationResponse

    // 2. Verify code
    @FormUrlEncoded
    @POST("xhr/index.php?f=verify_code")
    suspend fun verifyCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("code") code: String,
        @Field("username") username: String? = null,
        @Field("user_id") userId: Long? = null
    ): VerifyCodeResponse

    // 3. Resend verification code
    @FormUrlEncoded
    @POST("xhr/index.php?f=resend_verification_code")
    suspend fun resendVerificationCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("username") username: String? = null,
        @Field("user_id") userId: Long? = null
    ): ResendCodeResponse
}
```

---

## Data Models

```kotlin
// Response models
data class RegisterVerificationResponse(
    val api_status: Int,
    val message: String,
    val user_id: Long? = null,
    val username: String? = null,
    val verification_type: String? = null,
    val contact_info: String? = null,
    val code_length: Int? = null,
    val expires_in: Int? = null,
    val errors: String? = null
)

data class VerifyCodeResponse(
    val api_status: Int,
    val message: String,
    val user_id: Long? = null,
    val access_token: String? = null,
    val timezone: String? = null,
    val errors: String? = null
)

data class ResendCodeResponse(
    val api_status: Int,
    val message: String,
    val code_length: Int? = null,
    val expires_in: Int? = null,
    val errors: String? = null
)
```

---

## ViewModel Implementation

### RegisterViewModel

```kotlin
class RegisterViewModel(private val repository: UserRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState = _registerState.asStateFlow()

    fun registerWithEmail(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState.Loading

                val response = repository.registerWithVerification(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    verificationType = "email",
                    email = email,
                    phoneNumber = null
                )

                if (response.api_status == 200) {
                    _registerState.value = RegisterState.Success(response)
                } else {
                    _registerState.value = RegisterState.Error(
                        response.errors ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    fun registerWithPhone(
        username: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState.Loading

                val response = repository.registerWithVerification(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
                    verificationType = "phone",
                    email = null,
                    phoneNumber = phoneNumber
                )

                if (response.api_status == 200) {
                    _registerState.value = RegisterState.Success(response)
                } else {
                    _registerState.value = RegisterState.Error(
                        response.errors ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val response: RegisterVerificationResponse) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
```

### VerificationViewModel

```kotlin
class VerificationViewModel(private val repository: UserRepository) : ViewModel() {

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState = _verificationState.asStateFlow()

    private val _resendTimer = MutableStateFlow(60)
    val resendTimer = _resendTimer.asStateFlow()

    fun verifyCode(
        verificationType: String,
        contactInfo: String,
        code: String,
        username: String
    ) {
        viewModelScope.launch {
            try {
                _verificationState.value = VerificationState.Loading

                val response = repository.verifyCode(
                    verificationType = verificationType,
                    contactInfo = contactInfo,
                    code = code,
                    username = username
                )

                if (response.api_status == 200) {
                    // Сохраняем токен доступа
                    response.access_token?.let { token ->
                        UserSession.accessToken = token
                        UserSession.userId = response.user_id ?: 0L
                        UserSession.isLoggedIn = true
                    }

                    _verificationState.value = VerificationState.Success
                } else {
                    _verificationState.value = VerificationState.Error(
                        response.errors ?: "Verification failed"
                    )
                }
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    fun resendCode(
        verificationType: String,
        contactInfo: String,
        username: String
    ) {
        viewModelScope.launch {
            try {
                val response = repository.resendVerificationCode(
                    verificationType = verificationType,
                    contactInfo = contactInfo,
                    username = username
                )

                if (response.api_status == 200) {
                    // Запускаем таймер обратного отсчета
                    startResendTimer()
                } else {
                    _verificationState.value = VerificationState.Error(
                        response.errors ?: "Failed to resend code"
                    )
                }
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _resendTimer.value = i
                delay(1000)
            }
        }
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    object Success : VerificationState()
    data class Error(val message: String) : VerificationState()
}
```

---

## Usage Example (RegisterActivity)

```kotlin
class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegisterViewModel
    private lateinit var verificationViewModel: VerificationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)
        verificationViewModel = ViewModelProvider(this).get(VerificationViewModel::class.java)

        setContent {
            WorldMatesThemedApp {
                var showVerification by remember { mutableStateOf(false) }
                var registrationData by remember { mutableStateOf<RegisterVerificationResponse?>(null) }

                if (!showVerification) {
                    RegisterScreen(
                        viewModel = viewModel,
                        onRegistrationSuccess = { response ->
                            registrationData = response
                            showVerification = true
                        }
                    )
                } else {
                    VerificationScreen(
                        viewModel = verificationViewModel,
                        verificationType = registrationData?.verification_type ?: "email",
                        contactInfo = registrationData?.contact_info ?: "",
                        username = registrationData?.username ?: "",
                        onVerificationSuccess = {
                            navigateToChats()
                        },
                        onBackPressed = {
                            showVerification = false
                        }
                    )
                }
            }
        }

        // Observe registration state
        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Success -> {
                        // Registration successful, show verification screen
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
    }

    private fun navigateToChats() {
        startActivity(Intent(this, ChatsActivity::class.java))
        finish()
    }
}
```

---

## Testing

### 1. Test Registration (Email)

```kotlin
// In your test file
@Test
fun testRegisterWithEmail() = runBlocking {
    val response = api.registerWithVerification(
        username = "test_user",
        password = "password123",
        confirmPassword = "password123",
        verificationType = "email",
        email = "test@example.com"
    )

    assertEquals(200, response.api_status)
    assertNotNull(response.user_id)
}
```

### 2. Test Verification

```kotlin
@Test
fun testVerifyCode() = runBlocking {
    val response = api.verifyCode(
        verificationType = "email",
        contactInfo = "test@example.com",
        code = "123456",
        username = "test_user"
    )

    assertEquals(200, response.api_status)
    assertNotNull(response.access_token)
}
```

---

## Configuration

Add to your `build.gradle`:

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
}
```

---

## Notes

- All API calls should be made in coroutines
- Use ViewModels to manage API state
- Always handle errors gracefully
- Show loading indicators during API calls
- Validate inputs before making API calls
- Save access_token securely (use EncryptedSharedPreferences)

---

## Troubleshooting

### Common Issues

1. **401 Unauthorized** - Invalid or expired access token
2. **400 Bad Request** - Missing or invalid parameters
3. **Network timeout** - Check internet connection
4. **SMS not received** - Check Twilio/Infobip configuration

### Debug Mode

Enable logging in RetrofitClient:

```kotlin
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()
```

---

## Support

For questions or issues:
- GitHub: https://github.com/dncdante911/
- Email: support@worldmates.club
