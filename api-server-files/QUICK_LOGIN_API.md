# 🚀 Quick Login API - Быстрая авторизация через Email

## 📋 Описание

API для быстрой авторизации пользователей через email без пароля.
Пользователь вводит email → получает 6-значный код → вводит код → получает токен доступа.

**Использует рабочий SMTP из админки WoWonder!**

---

## 🔌 API Endpoints

### 1️⃣ Отправить код на email

**URL:** `https://worldmates.club/api/v2/?type=send_login_code`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com
```

**Успешный ответ (200):**
```json
{
    "api_status": 200,
    "message": "Login code sent to your email",
    "email": "user@example.com",
    "expires_in": 600
}
```

**Ошибки:**
```json
{
    "api_status": 400,
    "error_code": 6,
    "errors": {
        "error_id": 6,
        "error_text": "Email not found"
    }
}
```

---

### 2️⃣ Проверить код и войти

**URL:** `https://worldmates.club/api/v2/?type=verify_login_code`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com
code: 123456
```

**Успешный ответ (200):**
```json
{
    "api_status": 200,
    "message": "Login successful",
    "access_token": "abc123...xyz",
    "user_id": 42,
    "user_data": {
        "user_id": "42",
        "username": "john_doe",
        "email": "user@example.com",
        "name": "John Doe",
        "avatar": "https://...",
        "cover": "https://...",
        "verified": "0"
    }
}
```

**Ошибки:**
```json
{
    "api_status": 400,
    "error_code": 8,
    "errors": {
        "error_id": 8,
        "error_text": "Invalid code"
    }
}
```

---

## 📧 Настройка шаблона письма

### Где изменить шаблон:

**Файл:** `api-server-files/api/v2/endpoints/send-login-code.php`

**Строки:** 40-52 (переменная `$body`)

### Пример кастомизации:

```php
// Изменить цвет кода
$body .= '<span style="font-size: 32px; font-weight: bold; color: #FF5722; ...

// Изменить текст
$body .= '<p style="...">Ваш код для входа:</p>';

// Добавить логотип
$body .= '<img src="https://worldmates.club/logo.png" alt="Logo" style="max-width: 200px;" />';

// Изменить стили
$body .= '<div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ...
```

---

## 🧪 Тестирование

### Шаг 1: Отправить код
```bash
curl -X POST "https://worldmates.club/api/v2/?type=send_login_code" \
  -d "server_key=a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510" \
  -d "email=testik@sthost.pro"
```

### Шаг 2: Проверить почту
Откройте email и скопируйте 6-значный код (например: `456789`)

### Шаг 3: Войти с кодом
```bash
curl -X POST "https://worldmates.club/api/v2/?type=verify_login_code" \
  -d "server_key=a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510" \
  -d "email=testik@sthost.pro" \
  -d "code=456789"
```

### Шаг 4: Использовать токен
```bash
curl -X POST "https://worldmates.club/api/v2/?type=get_user_data" \
  -d "server_key=..." \
  -d "access_token=abc123...xyz"
```

---

## ⚙️ Технические детали

### Безопасность
- ✅ Код действителен 10 минут
- ✅ Одноразовый код (удаляется после использования)
- ✅ Проверка существования email
- ✅ Использует существующий SMTP (не требует настройки)

### Поля БД
- `sms_code` - хранит 6-значный код
- `email_code` - хранит timestamp истечения
- `access_token` - генерируется после успешной проверки

### Email отправка
Использует функцию `Wo_SendMessage()` из WoWonder:
- Автоматически использует настройки SMTP из админки
- Поддержка HTML шаблонов
- UTF-8 encoding
- Красивый дизайн письма

---

## 🔧 Установка на сервер

```bash
# Скопировать файлы
scp api-server-files/api/v2/endpoints/send-login-code.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/endpoints/verify-login-code.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/index.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

---

## 📱 Пример использования в приложении

```javascript
// 1. Запросить код
const response1 = await fetch('https://worldmates.club/api/v2/?type=send_login_code', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_server_key',
    email: userEmail
  })
});

const result1 = await response1.json();
if (result1.api_status === 200) {
  // Показать экран ввода кода
  showCodeInput();
}

// 2. Проверить код
const response2 = await fetch('https://worldmates.club/api/v2/?type=verify_login_code', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_server_key',
    email: userEmail,
    code: userCode
  })
});

const result2 = await response2.json();
if (result2.api_status === 200) {
  // Сохранить токен и войти
  localStorage.setItem('access_token', result2.access_token);
  navigateToHome();
}
```

---

## 🎨 Дизайн письма

Письмо содержит:
- 📧 Приветствие с именем пользователя
- 🔢 Крупный 6-значный код (зеленый, жирный)
- ⏱️ Информация об истечении (10 минут)
- 🛡️ Предупреждение о безопасности
- 🎨 Современный responsive дизайн

---

## ❓ FAQ

**Q: Можно ли изменить время действия кода?**
A: Да, в файле `send-login-code.php` измените `$expires_at = time() + 600;` (600 = 10 минут)

**Q: Как добавить логотип в письмо?**
A: Добавьте `<img src="URL_LOGO">` в переменную `$body` в файле `send-login-code.php`

**Q: Можно ли использовать SMS вместо email?**
A: Да, нужно добавить функцию `Wo_SendSMSMessage()` (аналогично `send-reset-password-email.php`)

**Q: Код можно использовать несколько раз?**
A: Нет, код одноразовый - удаляется после успешной проверки

---

## 📝 Коды ошибок

| Код | Описание |
|-----|----------|
| 3   | email не указан |
| 4   | code не указан |
| 6   | Email не найден |
| 7   | Не удалось отправить email |
| 8   | Неверный код |
| 9   | Код истек |

---

✅ **Готово к использованию!** SMTP уже работает, просто загрузите файлы на сервер!
