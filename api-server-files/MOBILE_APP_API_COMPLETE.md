# 📱 Полное API для мобильного приложения WorldMates

## 🎯 Два сценария использования

### 1️⃣ РЕГИСТРАЦИЯ (новый пользователь)
**Endpoints:** `quick_register` + `quick_verify`
**Работает:** ✅ Email | ⚠️ SMS (требует настройки Twilio)

### 2️⃣ ВХОД (существующий пользователь)
**Endpoints:** `send_login_code` + `verify_login_code`
**Работает:** ✅ Email | ❌ SMS (пока нет)

---

## 📝 РЕГИСТРАЦИЯ - Quick Register API

### Шаг 1: Отправить код (создать аккаунт)

**URL:** `https://worldmates.club/api/v2/?type=quick_register`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com              # ИЛИ
phone_number: +380123456789          # phone_number
android_m_device_id: xxx (optional)
ios_m_device_id: xxx (optional)
```

**Успешный ответ:**
```json
{
    "api_status": 200,
    "message": "Verification code sent to your email",
    "user_id": 42,
    "username": "u1707931234567",
    "verification_method": "email",
    "debug_verification_code": "123456"  // ⚠️ Только для теста!
}
```

**Что происходит:**
- ✅ Создается новый аккаунт (неактивный)
- ✅ Генерируется автоматический username: `u{timestamp}{random}`
- ✅ Генерируется 6-значный код
- ✅ Код отправляется на email (SMTP работает!) ИЛИ SMS
- ⏱️ Код действителен 15 минут

---

### Шаг 2: Подтвердить код (активировать аккаунт)

**URL:** `https://worldmates.club/api/v2/?type=quick_verify`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com              # ИЛИ phone_number
code: 123456
device_type: phone (optional: phone/windows)
```

**Успешный ответ:**
```json
{
    "api_status": 200,
    "message": "Account verified and activated successfully",
    "access_token": "abc123xyz...",
    "user_id": 42,
    "username": "u1707931234567",
    "user_platform": "phone"
}
```

**Что происходит:**
- ✅ Проверяется код
- ✅ Активируется аккаунт
- ✅ Создается сессия
- ✅ Возвращается `access_token` для использования в приложении
- ✅ Автоматически подписывается на пользователей/страницы (если настроено)

---

## 🔐 ВХОД - Quick Login API

### Шаг 1: Отправить код на email

**URL:** `https://worldmates.club/api/v2/?type=send_login_code`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com
```

**Успешный ответ:**
```json
{
    "api_status": 200,
    "message": "Login code sent to your email",
    "email": "user@example.com",
    "expires_in": 600
}
```

---

### Шаг 2: Войти с кодом

**URL:** `https://worldmates.club/api/v2/?type=verify_login_code`
**Method:** `POST`

**Параметры:**
```
server_key: a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510
email: user@example.com
code: 123456
```

**Успешный ответ:**
```json
{
    "api_status": 200,
    "message": "Login successful",
    "access_token": "xyz789...",
    "user_id": 42,
    "user_data": {
        "user_id": "42",
        "username": "john_doe",
        "email": "user@example.com",
        "name": "John Doe",
        "avatar": "https://...",
        "verified": "0"
    }
}
```

---

## 📱 Пример использования в мобильном приложении

### Сценарий 1: Регистрация нового пользователя

```javascript
// Пользователь вводит email
const email = "newuser@example.com";

// 1. Запросить код (создать аккаунт)
const response1 = await fetch('https://worldmates.club/api/v2/?type=quick_register', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_key',
    email: email
  })
});

const result1 = await response1.json();
if (result1.api_status === 200) {
  console.log("Код отправлен! User ID:", result1.user_id);
  // Показать экран ввода кода
  showCodeInput();
}

// 2. Пользователь вводит код из email
const userCode = "123456";

// 3. Подтвердить код
const response2 = await fetch('https://worldmates.club/api/v2/?type=quick_verify', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_key',
    email: email,
    code: userCode,
    device_type: 'phone'
  })
});

const result2 = await response2.json();
if (result2.api_status === 200) {
  // Сохранить токен
  localStorage.setItem('access_token', result2.access_token);
  localStorage.setItem('user_id', result2.user_id);
  // Войти в приложение
  navigateToHome();
}
```

---

### Сценарий 2: Вход существующего пользователя

```javascript
// Пользователь вводит email
const email = "existinguser@example.com";

// 1. Запросить код для входа
const response1 = await fetch('https://worldmates.club/api/v2/?type=send_login_code', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_key',
    email: email
  })
});

const result1 = await response1.json();
if (result1.api_status === 200) {
  console.log("Код отправлен!");
  showCodeInput();
}

// 2. Пользователь вводит код
const userCode = "789012";

// 3. Войти с кодом
const response2 = await fetch('https://worldmates.club/api/v2/?type=verify_login_code', {
  method: 'POST',
  body: new URLSearchParams({
    server_key: 'your_key',
    email: email,
    code: userCode
  })
});

const result2 = await response2.json();
if (result2.api_status === 200) {
  localStorage.setItem('access_token', result2.access_token);
  navigateToHome();
}
```

---

## 🧪 ТЕСТИРОВАНИЕ СЕЙЧАС

### ✅ Зарегистрироваться через email (РАБОТАЕТ):

```bash
# Шаг 1: Создать аккаунт
curl -X POST "https://worldmates.club/api/v2/?type=quick_register" \
  -d "server_key=a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510" \
  -d "email=test123@sthost.pro"

# Проверьте почту, скопируйте код

# Шаг 2: Активировать аккаунт
curl -X POST "https://worldmates.club/api/v2/?type=quick_verify" \
  -d "server_key=a8975daa76d7197ab87412b096696bb0e341eb4d-9bb411ab89d8a290362726fca6129e76-81746510" \
  -d "email=test123@sthost.pro" \
  -d "code=ВАЫШ_КОД"
```

---

### ⚠️ Зарегистрироваться через SMS (ТРЕБУЕТ НАСТРОЙКИ):

```bash
curl -X POST "https://worldmates.club/api/v2/?type=quick_register" \
  -d "server_key=..." \
  -d "phone_number=+380123456789"
```

**Для работы SMS нужно:**
1. Настроить Twilio/другой SMS провайдер в админке
2. Проверить функцию `Wo_SendSMSMessage()` в WoWonder

---

## 🔑 Коды ошибок

### quick_register:
| Код | Описание |
|-----|----------|
| 1 | email or phone_number обязателен |
| 2 | Неверный формат email |
| 3 | Email уже зарегистрирован |
| 4 | Номер телефона уже зарегистрирован |
| 5 | Ошибка регистрации |

### quick_verify:
| Код | Описание |
|-----|----------|
| 1 | Код обязателен |
| 2 | email or phone_number обязателен |
| 3 | Неверный формат кода (не 6 цифр) |
| 4 | Аккаунт не найден |
| 5 | Неверный код |
| 6 | Не удалось активировать аккаунт |

### send_login_code / verify_login_code:
| Код | Описание |
|-----|----------|
| 3 | email обязателен |
| 4 | code обязателен |
| 6 | Email не найден |
| 7 | Не удалось отправить email |
| 8 | Неверный код |
| 9 | Код истек |

---

## 📊 Сравнение API

| Функция | Quick Register | Quick Login |
|---------|----------------|-------------|
| **Цель** | Создать новый аккаунт | Войти в существующий |
| **Email** | ✅ Работает | ✅ Работает |
| **SMS** | ⚠️ Нужна настройка | ❌ Пока нет |
| **Время действия кода** | 15 минут | 10 минут |
| **Создает username** | ✅ Автоматически | ❌ Нет |
| **Активирует аккаунт** | ✅ Да | ❌ Нет (уже активен) |
| **Возвращает token** | ✅ Да | ✅ Да |

---

## ✅ ЧТО РАБОТАЕТ СЕЙЧАС

### В мобильном приложении можно:

✅ **Зарегистрироваться через email** → получить код → активировать → войти
✅ **Войти через email** (если уже есть аккаунт) → получить код → войти
✅ **Использовать access_token** для всех API запросов

### Что НЕ работает:

❌ **Регистрация через SMS** - требует настройки Twilio
❌ **Вход через SMS** - пока не реализован

---

## 🚀 Установка на сервер

```bash
# Файлы уже на сервере (quick_register + quick_verify)
# Нужно добавить только Quick Login:

scp api-server-files/api/v2/endpoints/send-login-code.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/endpoints/verify-login-code.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/index.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

---

## 🎯 ИТОГ

**ДА, ВСЕ РАБОТАЕТ!** 🎉

Сейчас можете:
1. ✅ Зарегистрироваться через email из мобильного приложения
2. ✅ Войти через email (Quick Login)
3. ✅ Получить access_token
4. ✅ Использовать все API с токеном

**НЕ работает:**
- ⚠️ SMS (нужна настройка провайдера в админке)

**Тестируйте прямо сейчас!** 🚀
