# 📦 WorldMates Messenger - SMS/Email Verification API

## ✅ Что было создано

Полный набор API endpoints для регистрации и верификации пользователей через SMS или Email.

### 📂 Файлы

**API Endpoints (PHP):**
- ✅ `register_with_verification.php` - Регистрация с автоматической отправкой кода
- ✅ `send_verification_code.php` - Отправка кода верификации
- ✅ `verify_code.php` - Проверка кода и активация аккаунта
- ✅ `resend_verification_code.php` - Повторная отправка кода

**Документация:**
- ✅ `VERIFICATION_API_DOCUMENTATION.md` - Полная документация API
- ✅ `ANDROID_INTEGRATION_GUIDE.md` - Руководство по интеграции с Android
- ✅ `README_VERIFICATION_API.md` - Общая информация и быстрый старт

---

## 🚀 Инструкция по развертыванию

### Шаг 1: Копирование файлов на сервер

```bash
# Скопируйте PHP файлы в директорию xhr/ на вашем сервере
scp server_api/*.php user@your-server:/path/to/project/xhr/

# Установите правильные права доступа
chmod 644 /path/to/project/xhr/*.php
```

### Шаг 2: Настройка SMS провайдера

**Вариант A: Twilio**

1. Зарегистрируйтесь на https://www.twilio.com/
2. Получите Account SID и Auth Token
3. Купите номер телефона
4. Настройте в админ-панели WorldMates:
   ```
   Settings → SMS Settings
   - SMS Provider: Twilio
   - Account SID: ACxxxxxxxxxxxxxxx
   - Auth Token: your_auth_token
   - Phone Number: +1234567890
   ```

**Вариант B: Infobip**

1. Зарегистрируйтесь на https://www.infobip.com/
2. Получите API Key
3. Настройте в админ-панели:
   ```
   Settings → SMS Settings
   - SMS Provider: Infobip
   - API Key: your_api_key
   - Base URL: https://api.infobip.com
   ```

### Шаг 3: Проверка базы данных

Убедитесь, что в таблице `users` есть поля:

```sql
-- Проверка
DESCRIBE users;

-- Если полей нет, добавьте их:
ALTER TABLE users ADD COLUMN email_code VARCHAR(32) DEFAULT '';
ALTER TABLE users ADD COLUMN sms_code INT(11) DEFAULT 0;
ALTER TABLE users ADD COLUMN phone_number VARCHAR(32) DEFAULT '';

-- Добавьте индексы для производительности
ALTER TABLE users ADD INDEX idx_phone_number (phone_number);
ALTER TABLE users ADD INDEX idx_email_code (email_code);
```

### Шаг 4: Тестирование API

**Тест 1: Регистрация через Email**

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=register_with_verification' \
  -d 'username=testuser' \
  -d 'password=test123456' \
  -d 'confirm_password=test123456' \
  -d 'verification_type=email' \
  -d 'email=test@example.com'
```

**Ожидаемый ответ:**
```json
{
  "api_status": 200,
  "message": "Registration successful! Verification code sent to your email",
  "user_id": 123,
  "username": "testuser",
  "verification_type": "email",
  "contact_info": "test@example.com",
  "code_length": 6,
  "expires_in": 600
}
```

**Тест 2: Проверка кода**

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=verify_code' \
  -d 'verification_type=email' \
  -d 'contact_info=test@example.com' \
  -d 'code=123456' \
  -d 'username=testuser'
```

**Ожидаемый ответ:**
```json
{
  "api_status": 200,
  "message": "Email verified successfully",
  "user_id": 123,
  "access_token": "abc123def456...",
  "timezone": "UTC"
}
```

---

## 📱 Интеграция с Android приложением

### Обновление API интерфейса

Добавьте в `app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt`:

```kotlin
interface WorldMatesApi {

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

    @FormUrlEncoded
    @POST("xhr/index.php?f=verify_code")
    suspend fun verifyCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("code") code: String,
        @Field("username") username: String? = null
    ): VerifyCodeResponse

    @FormUrlEncoded
    @POST("xhr/index.php?f=resend_verification_code")
    suspend fun resendVerificationCode(
        @Field("verification_type") verificationType: String,
        @Field("contact_info") contactInfo: String,
        @Field("username") username: String? = null
    ): ResendCodeResponse
}
```

### Модели данных

Создайте файл `app/src/main/java/com/worldmates/messenger/models/VerificationModels.kt`:

```kotlin
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

### Обновление RegisterViewModel

В `RegisterViewModel.kt` добавьте метод для регистрации:

```kotlin
fun registerWithPhone(
    username: String,
    phoneNumber: String,
    password: String,
    confirmPassword: String
) {
    viewModelScope.launch {
        try {
            _registerState.value = RegisterState.Loading

            val response = apiService.registerWithVerification(
                username = username,
                password = password,
                confirmPassword = confirmPassword,
                verificationType = "phone",
                phoneNumber = phoneNumber
            )

            if (response.api_status == 200) {
                _registerState.value = RegisterState.VerificationRequired(
                    userId = response.user_id ?: 0,
                    username = response.username ?: "",
                    verificationType = "phone",
                    contactInfo = phoneNumber
                )
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
```

### Обновление VerificationViewModel

Методы уже реализованы в `VerificationViewModel.kt`, но убедитесь что они используют новые endpoints:

```kotlin
fun verifyCode(
    verificationType: String,
    contactInfo: String,
    code: String
) {
    viewModelScope.launch {
        try {
            _verificationState.value = VerificationState.Loading

            val response = apiService.verifyCode(
                verificationType = verificationType,
                contactInfo = contactInfo,
                code = code,
                username = null
            )

            if (response.api_status == 200 && response.access_token != null) {
                // Сохраняем токен
                UserSession.accessToken = response.access_token
                UserSession.userId = response.user_id ?: 0L
                UserSession.isLoggedIn = true

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
    contactInfo: String
) {
    viewModelScope.launch {
        try {
            val response = apiService.resendVerificationCode(
                verificationType = verificationType,
                contactInfo = contactInfo,
                username = null
            )

            if (response.api_status == 200) {
                startResendTimer()
                _verificationState.value = VerificationState.CodeResent
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
```

---

## 🔧 Конфигурация сервера

### Apache Configuration

Убедитесь, что `.htaccess` разрешает POST запросы:

```apache
<IfModule mod_rewrite.c>
    RewriteEngine On
    RewriteBase /

    # Allow POST requests
    RewriteCond %{REQUEST_METHOD} !^(GET|POST|HEAD)$
    RewriteRule .* - [F]
</IfModule>
```

### PHP Configuration

Минимальные требования в `php.ini`:

```ini
post_max_size = 20M
upload_max_filesize = 20M
max_execution_time = 300
memory_limit = 256M

# Для отправки email
[mail function]
SMTP = smtp.your-provider.com
smtp_port = 587
sendmail_from = noreply@your-domain.com
```

---

## 🔒 Безопасность

### Rate Limiting (рекомендуется)

Создайте файл `rate_limit.php`:

```php
<?php
function check_rate_limit($action, $identifier, $max_attempts = 5, $time_window = 3600) {
    global $sqlConnect;

    $identifier = mysqli_real_escape_string($sqlConnect, $identifier);
    $action = mysqli_real_escape_string($sqlConnect, $action);
    $time_threshold = time() - $time_window;

    // Проверяем количество попыток
    $query = mysqli_query($sqlConnect,
        "SELECT COUNT(*) as attempts FROM rate_limits
         WHERE action = '{$action}'
         AND identifier = '{$identifier}'
         AND timestamp > {$time_threshold}"
    );

    $result = mysqli_fetch_assoc($query);

    if ($result['attempts'] >= $max_attempts) {
        return false; // Превышен лимит
    }

    // Записываем попытку
    mysqli_query($sqlConnect,
        "INSERT INTO rate_limits (action, identifier, timestamp)
         VALUES ('{$action}', '{$identifier}', " . time() . ")"
    );

    return true;
}
?>
```

Создайте таблицу в БД:

```sql
CREATE TABLE IF NOT EXISTS rate_limits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    identifier VARCHAR(100) NOT NULL,
    timestamp INT NOT NULL,
    INDEX idx_action_identifier (action, identifier),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Добавьте проверку в начало каждого endpoint:

```php
require_once('rate_limit.php');

$ip = get_ip_address();
if (!check_rate_limit('register', $ip, 5, 3600)) {
    $errors = 'Too many attempts. Please try again later.';
    echo json_encode(array('api_status' => 429, 'errors' => $errors));
    exit();
}
```

---

## ✅ Чеклист внедрения

### Серверная часть
- [ ] Скопированы все PHP файлы в `xhr/`
- [ ] Настроен SMS провайдер (Twilio или Infobip)
- [ ] Настроен SMTP для email
- [ ] Проверены/добавлены поля в БД
- [ ] Протестированы endpoints с curl
- [ ] Добавлена защита от злоупотреблений (rate limiting)
- [ ] Настроены права доступа к файлам (644)

### Android приложение
- [ ] Обновлен `WorldMatesApi.kt`
- [ ] Созданы модели данных (`VerificationModels.kt`)
- [ ] Обновлен `RegisterViewModel.kt`
- [ ] Обновлен `VerificationViewModel.kt`
- [ ] Протестирована регистрация через Email
- [ ] Протестирована регистрация через Phone
- [ ] Протестирована повторная отправка кода
- [ ] Обработаны все ошибки

### Тестирование
- [ ] Регистрация Email → Верификация → Логин
- [ ] Регистрация Phone → Верификация → Логин
- [ ] Повторная отправка кода (Email)
- [ ] Повторная отправка кода (Phone)
- [ ] Неверный код (ошибка)
- [ ] Истекший код (ошибка)
- [ ] Дубликат username (ошибка)
- [ ] Дубликат email/phone (ошибка)

---

## 📚 Полезные ссылки

- **Полная документация API:** `VERIFICATION_API_DOCUMENTATION.md`
- **Руководство Android:** `ANDROID_INTEGRATION_GUIDE.md`
- **Twilio Docs:** https://www.twilio.com/docs/sms/quickstart
- **Infobip Docs:** https://www.infobip.com/docs/api#channels/sms

---

## 🆘 Поддержка и помощь

Если возникли проблемы:

1. Проверьте логи сервера: `tail -f /var/log/apache2/error.log`
2. Проверьте логи PHP: `tail -f /var/log/php-errors.log`
3. Включите debug mode в Retrofit (Android)
4. Проверьте документацию: `VERIFICATION_API_DOCUMENTATION.md`

**Контакты:**
- GitHub: https://github.com/dncdante911/
- Email: support@worldmates.club

---

## 🎉 Готово!

Теперь у вас есть полностью рабочая система верификации через SMS и Email!

**Следующие шаги:**
1. Скопируйте PHP файлы на сервер
2. Настройте SMS провайдера
3. Обновите Android приложение
4. Протестируйте все flows
5. Запустите в продакшн!

Удачи с проектом! 🚀
