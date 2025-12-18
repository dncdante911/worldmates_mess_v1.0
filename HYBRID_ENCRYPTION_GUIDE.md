# 🔐 Гибридное Шифрование WorldMates

## 📖 Концепция

**WorldMates** теперь поддерживает **гибридное шифрование** - одновременную работу двух типов приложений:

| Приложение | Шифрование | Безопасность | Совместимость |
|------------|------------|--------------|---------------|
| **WorldMates** (наш) | AES-256-GCM | 🔐🔐🔐 Топовая | Только WorldMates |
| **WoWonder Official** | AES-128-ECB | 🔓 Базовая | Совместимо с официальным |

---

## 🎯 Как это работает?

### Определение типа приложения:

**POST параметр:** `use_gcm=true`

- ✅ **Если есть** → WorldMates → AES-256-GCM
- ❌ **Если нет** → WoWonder Official → AES-128-ECB

### Схема работы:

```
┌─────────────────┐         ┌─────────────────┐
│  WorldMates App │         │ WoWonder Official│
│  use_gcm=true   │         │  (no parameter)  │
└────────┬────────┘         └────────┬─────────┘
         │                           │
         │ POST /send-message        │ POST /send-message
         │ use_gcm=true              │ (без use_gcm)
         │                           │
         ▼                           ▼
    ┌────────────────────────────────────┐
    │     send-message-hybrid.php        │
    │                                    │
    │  if (use_gcm == true)             │
    │    → AES-256-GCM + IV + Tag       │
    │  else                             │
    │    → AES-128-ECB (legacy)         │
    └────────────┬───────────────────────┘
                 │
                 ▼
         ┌──────────────┐
         │  Database    │
         │  cipher_version: 1 (ECB)    │
         │  cipher_version: 2 (GCM)    │
         │  iv, tag (только для GCM)   │
         └──────┬───────┘
                │
                ▼
         ┌──────────────┐
         │ get_user_messages.php        │
         │ Возвращает как есть с полями │
         └──────┬───────┘
                │
       ┌────────┴────────┐
       │                 │
       ▼                 ▼
  WorldMates      WoWonder Official
  Расшифровка     Расшифровка
  GCM (v2)        ECB (v1)
```

---

## 🛠️ Изменённые файлы:

### 1. Android App (WorldMates):

**RetrofitClient.kt** (ApiKeyInterceptor):
```kotlin
// Добавляем use_gcm=true для WorldMates (топовая защита AES-256-GCM)
formBodyBuilder.add("use_gcm", "true")
```

Теперь **каждый POST запрос** от WorldMates содержит `use_gcm=true`!

### 2. PHP Server:

#### send-message-hybrid.php:
```php
// Определяем тип клиента
$use_gcm = !empty($_POST['use_gcm']) && $_POST['use_gcm'] == 'true';

if ($use_gcm && class_exists('CryptoHelper')) {
    // WorldMates: AES-256-GCM
    $encrypted = CryptoHelper::encryptGCM($plaintext, $message_data['time']);
    $message_data['text'] = $encrypted['text'];
    $message_data['iv'] = $encrypted['iv'];
    $message_data['tag'] = $encrypted['tag'];
    $message_data['cipher_version'] = 2;
} else {
    // Official WoWonder: AES-128-ECB
    $message_data['text'] = openssl_encrypt($plaintext, "AES-128-ECB", $message_data['time']);
    $message_data['cipher_version'] = 1;
}
```

#### group_chat_v2.php:
```php
// HYBRID: Визначаємо тип клієнта
$use_gcm = !empty($_POST['use_gcm']) && $_POST['use_gcm'] == 'true';

if ($use_gcm) {
    // WorldMates: AES-256-GCM
    $encrypted_data = CryptoHelper::encryptGCM($text, $time);
    $encrypted_text = $encrypted_data['text'];
    $iv = $encrypted_data['iv'];
    $tag = $encrypted_data['tag'];
    $cipher_version = 2;
} else {
    // Офіційний WoWonder: AES-128-ECB
    $encrypted_text = openssl_encrypt($text, "AES-128-ECB", $time);
    $cipher_version = 1;
}
```

#### get_user_messages.php:
```php
// Сообщения уже зашифрованы в БД с GCM, возвращаем как есть
// Поля iv, tag, cipher_version уже есть в $message из БД

// НЕТ повторного шифрования!
```

---

## 📦 Установка:

### 1. Скопируйте PHP файлы на сервер:

```bash
# Hybrid send-message
scp /home/user/api-worldmates/send-message-hybrid.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/send-message.php

# Group chat v2 (уже обновлён)
scp /home/user/worldmates_mess_v1.0/server_modifications/group_chat_v2.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/

# Get messages (убрана повторная шифровка)
scp /home/user/api-worldmates/get_user_messages.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

# Crypto helper (если ещё нет)
scp /home/user/api-worldmates/includes/crypto_helper.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

### 2. Соберите Android приложение:

```bash
cd /home/user/worldmates_mess_v1.0
./gradlew assembleDebug
```

---

## 🧪 Тестирование:

### Сценарий 1: WorldMates → WorldMates

1. Отправьте сообщение из WorldMates: "Test GCM 🔐"
2. Проверьте в БД:
```sql
SELECT id, from_id, to_id,
       SUBSTRING(text, 1, 30) as encrypted_text,
       iv, tag, cipher_version
FROM Wo_Messages
ORDER BY id DESC LIMIT 1;
```

**Ожидаемый результат:**
```
cipher_version = 2 (GCM)
iv = base64 string (12 байт)
tag = base64 string (16 байт)
text = base64 encrypted
```

3. Получатель в WorldMates видит: "Test GCM 🔐" ✅

### Сценарий 2: WoWonder Official → WorldMates

1. Отправьте сообщение из официального WoWonder: "Test ECB"
2. Проверьте в БД:
```sql
SELECT cipher_version, iv, tag FROM Wo_Messages ORDER BY id DESC LIMIT 1;
```

**Ожидаемый результат:**
```
cipher_version = 1 (ECB)
iv = NULL
tag = NULL
```

3. Получатель в WorldMates видит: "Test ECB" ✅ (DecryptionUtility авто-определяет ECB)

### Сценарий 3: WorldMates → WoWonder Official

1. WorldMates отправляет: "Test Hybrid"
2. В БД: `cipher_version = 2, iv и tag заполнены`
3. WoWonder Official НЕ СМОЖЕТ расшифровать (не поддерживает GCM) ❌

**Решение:** Официальное приложение WoWonder должно быть обновлено для поддержки GCM, или пользователи должны использовать только WorldMates.

---

## 🔒 Безопасность:

| Метод | Уязвимости | Защита |
|-------|-----------|--------|
| **AES-128-ECB** | ❌ Детерминированность<br>❌ Паттерны видны<br>❌ Без аутентификации | 🔓 Низкая |
| **AES-256-GCM** | ✅ Случайный IV<br>✅ Аутентификация<br>✅ 256-бит ключ | 🔐 Высокая |

**Рекомендация:** Используйте WorldMates для максимальной безопасности!

---

## 📊 База данных:

### Структура таблицы Wo_Messages:

```sql
ALTER TABLE Wo_Messages ADD COLUMN iv VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN tag VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN cipher_version INT DEFAULT 1;
```

### Значения cipher_version:

- `1` = AES-128-ECB (legacy, WoWonder Official)
- `2` = AES-256-GCM (современный, WorldMates)

---

## 🎯 Преимущества:

✅ **Обратная совместимость** - официальное приложение продолжает работать
✅ **Постепенная миграция** - пользователи могут переходить на WorldMates
✅ **Топовая защита** - WorldMates использует лучшее шифрование
✅ **Прозрачность** - автоматическое определение типа шифрования
✅ **Гибкость** - можно отключить GCM убрав `use_gcm` параметр

---

## 🚀 Итого:

Теперь у вас **гибридная система**:

- **WorldMates** = AES-256-GCM (топовая защита) 🔐
- **WoWonder Official** = AES-128-ECB (совместимость) 🔓

Оба работают одновременно на одном сервере! 🎉
