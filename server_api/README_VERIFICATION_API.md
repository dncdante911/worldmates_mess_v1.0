# WorldMates Messenger - SMS/Email Verification API

## 📦 Что включено

Этот набор API endpoints обеспечивает полную функциональность регистрации и верификации пользователей через SMS или Email.

### API Endpoints

1. **`register_with_verification.php`** - Регистрация с автоматической отправкой кода
2. **`send_verification_code.php`** - Отправка кода верификации
3. **`verify_code.php`** - Проверка кода и активация аккаунта
4. **`resend_verification_code.php`** - Повторная отправка кода

### Документация

- **`VERIFICATION_API_DOCUMENTATION.md`** - Полная документация API с примерами
- **`ANDROID_INTEGRATION_GUIDE.md`** - Руководство по интеграции с Android приложением

---

## 🚀 Быстрый старт

### 1. Установка

Скопируйте все PHP файлы в директорию `xhr/` вашего проекта:

```bash
cp send_verification_code.php /path/to/your/project/xhr/
cp verify_code.php /path/to/your/project/xhr/
cp resend_verification_code.php /path/to/your/project/xhr/
cp register_with_verification.php /path/to/your/project/xhr/
```

### 2. Конфигурация

Убедитесь, что в админ-панели настроен SMS провайдер:

**Для Twilio:**
```
Settings → SMS Settings
- SMS Provider: Twilio
- Account SID: your_account_sid
- Auth Token: your_auth_token
- Phone Number: +1234567890
```

**Для Infobip:**
```
Settings → SMS Settings
- SMS Provider: Infobip
- API Key: your_api_key
- Base URL: https://api.infobip.com
```

### 3. Проверка

Проверьте базу данных - должны быть поля:
- `users.email_code` (varchar 32)
- `users.sms_code` (int 11)
- `users.phone_number` (varchar 32)

---

## 📱 Интеграция с Android

### Обновите API интерфейс

Добавьте в `WorldMatesApi.kt`:

```kotlin
@FormUrlEncoded
@POST("xhr/index.php?f=register_with_verification")
suspend fun registerWithVerification(
    @Field("username") username: String,
    @Field("password") password: String,
    @Field("confirm_password") confirmPassword: String,
    @Field("verification_type") verificationType: String,
    @Field("email") email: String? = null,
    @Field("phone_number") phoneNumber: String? = null
): RegisterVerificationResponse

@FormUrlEncoded
@POST("xhr/index.php?f=verify_code")
suspend fun verifyCode(
    @Field("verification_type") verificationType: String,
    @Field("contact_info") contactInfo: String,
    @Field("code") code: String,
    @Field("username") username: String
): VerifyCodeResponse

@FormUrlEncoded
@POST("xhr/index.php?f=resend_verification_code")
suspend fun resendVerificationCode(
    @Field("verification_type") verificationType: String,
    @Field("contact_info") contactInfo: String,
    @Field("username") username: String
): ResendCodeResponse
```

Подробнее см. `ANDROID_INTEGRATION_GUIDE.md`

---

## 🔄 Процесс верификации

### Email Verification Flow

```
User Registration
      ↓
[POST] register_with_verification (verification_type=email)
      ↓
Email sent with 6-digit code
      ↓
User enters code
      ↓
[POST] verify_code
      ↓
Account activated + Auto-login
```

### Phone Verification Flow

```
User Registration
      ↓
[POST] register_with_verification (verification_type=phone)
      ↓
SMS sent with 6-digit code
      ↓
User enters code
      ↓
[POST] verify_code
      ↓
Account activated + Auto-login
```

---

## 📝 Примеры запросов

### Регистрация через Email

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=register_with_verification' \
  -d 'username=john_doe' \
  -d 'password=securepass123' \
  -d 'confirm_password=securepass123' \
  -d 'verification_type=email' \
  -d 'email=john@example.com'
```

**Response:**
```json
{
  "api_status": 200,
  "message": "Registration successful! Verification code sent to your email",
  "user_id": 123,
  "username": "john_doe",
  "verification_type": "email",
  "contact_info": "john@example.com",
  "code_length": 6,
  "expires_in": 600
}
```

### Верификация кода

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=verify_code' \
  -d 'verification_type=email' \
  -d 'contact_info=john@example.com' \
  -d 'code=123456' \
  -d 'username=john_doe'
```

**Response:**
```json
{
  "api_status": 200,
  "message": "Email verified successfully",
  "user_id": 123,
  "access_token": "abc123def456...",
  "timezone": "UTC"
}
```

### Повторная отправка кода

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=resend_verification_code' \
  -d 'verification_type=email' \
  -d 'contact_info=john@example.com' \
  -d 'username=john_doe'
```

---

## 🛡️ Безопасность

### Защита от злоупотреблений

Рекомендуется добавить:

1. **Rate Limiting** - ограничение количества запросов
2. **CAPTCHA** - для защиты от ботов
3. **IP Blocking** - блокировка подозрительных IP
4. **Code Expiration** - коды истекают через 10 минут

### Пример добавления Rate Limiting

```php
// В начале каждого endpoint
$ip = get_ip_address();
$attempts = get_attempts_count($ip);

if ($attempts > 5) {
    $errors = "Too many attempts. Please try again later.";
    echo json_encode(array('api_status' => 429, 'errors' => $errors));
    exit();
}

record_attempt($ip);
```

---

## 🔧 Troubleshooting

### SMS не доставляется

1. Проверьте настройки Twilio/Infobip
2. Проверьте баланс на аккаунте SMS провайдера
3. Убедитесь, что номер телефона в международном формате (+380...)
4. Проверьте логи: `tail -f /var/log/apache2/error.log`

### Email не доставляется

1. Проверьте настройки SMTP
2. Проверьте папку "Спам"
3. Убедитесь, что email не в черном списке
4. Проверьте SPF/DKIM записи

### Ошибка "User not found"

1. Убедитесь, что пользователь был создан
2. Проверьте таблицу `users` в БД
3. Проверьте, что `username` или `user_id` передаются правильно

### Ошибка "Wrong confirmation code"

1. Проверьте, что код не истек (10 минут)
2. Убедитесь, что код введен правильно
3. Проверьте значение в БД: `SELECT sms_code, email_code FROM users WHERE user_id = X`

---

## 📚 Дополнительные ресурсы

- **API Documentation:** `VERIFICATION_API_DOCUMENTATION.md`
- **Android Integration:** `ANDROID_INTEGRATION_GUIDE.md`
- **Twilio Docs:** https://www.twilio.com/docs/sms
- **Infobip Docs:** https://www.infobip.com/docs/api

---

## 🤝 Поддержка

Для вопросов и поддержки:

- GitHub Issues: https://github.com/dncdante911/worldmates_mess_v1.0/issues
- Email: support@worldmates.club
- Telegram: @worldmates_support

---

## 📄 Лицензия

MIT License - используйте свободно в своих проектах.

---

## ✅ Чеклист перед запуском

- [ ] Скопированы все PHP файлы в `xhr/`
- [ ] Настроен SMS провайдер (Twilio или Infobip)
- [ ] Настроен SMTP для email
- [ ] Проверены поля в БД (`email_code`, `sms_code`, `phone_number`)
- [ ] Обновлен Android API интерфейс
- [ ] Протестирована регистрация через Email
- [ ] Протестирована регистрация через Phone
- [ ] Протестирована повторная отправка кода
- [ ] Добавлена защита от злоупотреблений

---

## 🎉 Готово!

Теперь у вас есть полнофункциональная система верификации через SMS и Email!

Удачи с проектом! 🚀
