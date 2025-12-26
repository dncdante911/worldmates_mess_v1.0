# 🔧 Виправлення проблем реального часу в мессенджері

## 📋 Опис проблеми

В мессенджері не працювало оновлення повідомлень та статусів в реальному часі:
- ❌ Не показувався статус "онлайн"
- ❌ Не показувався статус "оффлайн"
- ❌ Не показувався індикатор "печатає..."
- ❌ Повідомлення не оновлювались автоматично

## 🔍 Знайдені причини

### 1. Несумісність назв Socket.IO подій

**Клієнт (Android) очікував:**
```kotlin
SOCKET_EVENT_USER_ONLINE = "on_user_loggedin"
SOCKET_EVENT_USER_OFFLINE = "on_user_loggedoff"
```

**Сервер (Node.js/WoWonder) відправляв:**
```javascript
socket.emit("user_status_change", {
    user_id: userId,
    status: "1" // або "0"
})
```

### 2. Неповна відправка даних при події "typing"

Клієнт не відправляв поле `is_typing` з правильними значеннями (200/300), які очікує WoWonder сервер.

## ✅ Виправлення

### 1. Додано обробник події `user_status_change` в SocketManager.kt

**Файл:** `app/src/main/java/com/worldmates/messenger/network/SocketManager.kt`

```kotlin
// 14. КРИТИЧНО: Обработка события "user_status_change" от WoWonder сервера
socket?.on("user_status_change") { args ->
    Log.d("SocketManager", "Received user_status_change event with ${args.size} args")
    if (args.isNotEmpty()) {
        if (args[0] is JSONObject) {
            val data = args[0] as JSONObject
            val userId = data.optLong("user_id", 0)
            // Проверяем статус: 0 = offline, 1 = online
            val status = data.optString("status", "0")
            val isOnline = status == "1" || status.equals("online", ignoreCase = true)

            Log.d("SocketManager", "User $userId status changed: ${if (isOnline) "ONLINE ✅" else "OFFLINE ❌"}")

            if (listener is ExtendedSocketListener) {
                if (isOnline) {
                    listener.onUserOnline(userId)
                } else {
                    listener.onUserOffline(userId)
                }
            }
        }
    }
}
```

**Що це робить:**
- ✅ Слухає подію `user_status_change` від сервера
- ✅ Парсить `user_id` та `status` (0=offline, 1=online)
- ✅ Викликає відповідні callback методи `onUserOnline()` або `onUserOffline()`

### 2. Виправлено відправку події "typing" в MessagesViewModel.kt

**Файл:** `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesViewModel.kt`

```kotlin
fun sendTypingStatus(isTyping: Boolean) {
    if (recipientId == 0L) return

    socketManager?.emit(Constants.SOCKET_EVENT_TYPING, JSONObject().apply {
        put("user_id", UserSession.userId)  // Кто печатает
        put("recipient_id", recipientId)  // Кому отправляем
        // Формат WoWonder: is_typing = 200 (печатает) или 300 (закончил)
        put("is_typing", if (isTyping) 200 else 300)
    })
    Log.d("MessagesViewModel", "Відправлено статус 'печатає': $isTyping для користувача $recipientId")
}
```

**Що змінилось:**
- ✅ Додано перевірку на `recipientId == 0L`
- ✅ Додано поле `is_typing` з правильними значеннями (200/300)
- ✅ Додано логування для відладки

## 📊 Формат подій Socket.IO

### Події від сервера до клієнта:

| Подія | Формат даних | Опис |
|-------|--------------|------|
| `user_status_change` | `{user_id: Long, status: "0"\|"1"}` | Зміна статусу онлайн/оффлайн |
| `typing` | `{sender_id: Long, is_typing: 200\|300}` | Користувач друкує (200) або закінчив (300) |
| `private_message` | `{id, from_id, to_id, text, time, ...}` | Нове особисте повідомлення |
| `group_message` | `{id, from_id, group_id, text, time, ...}` | Нове групове повідомлення |

### Події від клієнта до сервера:

| Подія | Формат даних | Опис |
|-------|--------------|------|
| `join` | `{user_id: String (access_token)}` | Автентифікація при підключенні |
| `typing` | `{user_id: Long, recipient_id: Long, is_typing: 200\|300}` | Відправка статусу друкування |
| `private_message` | `{msg: String, from_id: Long, to_id: Long}` | Відправка особистого повідомлення |

## 🧪 Тестування

### Як перевірити, що працює:

1. **Статус "онлайн/оффлайн":**
   - Відкрийте чат з користувачем
   - Попросіть його увійти/вийти з додатку
   - Статус повинен змінюватись автоматично

2. **Індикатор "печатає":**
   - Відкрийте чат
   - Почніть друкувати повідомлення
   - Співрозмовник повинен побачити "печатає..."

3. **Отримання повідомлень:**
   - Надішліть повідомлення з іншого пристрою
   - Воно повинно з'явитись автоматично без перезавантаження

### Логи для відладки:

```bash
adb logcat | grep -E "SocketManager|MessagesViewModel"
```

**Що шукати в логах:**
- `✅ User XXX is ONLINE` - користувач онлайн
- `❌ User XXX is OFFLINE` - користувач оффлайн
- `User XXX status changed: ONLINE ✅` - отримано подію зміни статусу
- `Відправлено статус 'печатає': true` - відправлено індикатор друкування
- `Додано нове повідомлення від Socket.IO` - отримано повідомлення

## 📝 Додаткові рекомендації

### Якщо проблеми все ще є:

1. **Перевірте підключення Socket.IO:**
   ```bash
   adb logcat | grep "Socket Connected"
   ```
   Повинно бути: `Socket Connected! ID: XXX`

2. **Перевірте URL сервера:**
   У `Constants.kt` перевірте:
   ```kotlin
   const val SOCKET_URL = "https://worldmates.club:449/"
   ```

3. **Перевірте Node.js сервер:**
   - Переконайтесь, що Node.js сервер запущений на порту 449
   - Перевірте, що SSL сертифікат валідний
   - Перевірте логи сервера на наявність підключень

4. **Перевірте HAproxy:**
   Якщо використовується HAproxy, переконайтесь що WebSocket з'єднання проксуються правильно:
   ```
   option http-server-close
   option forwardfor
   ```

## 🔗 Пов'язані файли

- `app/src/main/java/com/worldmates/messenger/network/SocketManager.kt` - менеджер Socket.IO
- `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesViewModel.kt` - ViewModel для чату
- `app/src/main/java/com/worldmates/messenger/data/Constants.kt` - константи для Socket.IO
- `app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt` - UI чату

## 📚 Документація WoWonder

Використані події базуються на офіційній документації WoWonder Messenger API:
- События Socket.IO: `events.js`
- Слухачі подій: `listeners.js`
- Конфігурація: `config.json`

## 🔧 Додаткові виправлення (v2)

### Проблеми після першого виправлення:

1. **XHR polling помилки** - Socket.IO використовував XHR замість WebSocket
2. **HTML замість JSON** - сервер відправляв HTML розмітку в події `user_status_change`
3. **Втрата статусу онлайн** - статус скидався при друкуванні

### Виправлення v2:

#### 1. Форсування WebSocket транспорту
```kotlin
opts.transports = arrayOf("websocket", "polling")
```

#### 2. Парсинг HTML від WoWonder
```kotlin
private fun parseOnlineUsers(html: String, isOnline: Boolean) {
    val pattern = """id="online_(\d+)"""".toRegex()
    val matches = pattern.findAll(html)
    matches.forEach { match ->
        val userId = match.groupValues[1].toLongOrNull()
        // обробка userId
    }
}
```

#### 3. Запобігання втраті статусу
```kotlin
override fun onTypingStatus(userId: Long, isTyping: Boolean) {
    if (isTyping) {
        _recipientOnlineStatus.value = true  // Друкує = онлайн!
    }
}

override fun onUserOffline(userId: Long) {
    if (!_isTyping.value) {  // Не скидаємо якщо друкує
        _recipientOnlineStatus.value = false
    }
}
```

#### 4. Додаткові події повідомлень
```kotlin
socket?.on("private_message_page") { ... }
socket?.on("page_message") { ... }
```

### Логи для відладки v2:

```bash
# WebSocket з'єднання
adb logcat | grep "websocket"

# Парсинг онлайн користувачів
adb logcat | grep "Parsed user"

# Отримання повідомлень
adb logcat | grep "📨"
```

---

**Дата створення:** 2025-12-26
**Останнє оновлення:** 2025-12-26 (v2)
**Автор:** Claude Code Agent
**Статус:** ✅ Виправлено (v2 - WebSocket + HTML парсинг)
