# 🧪 Тестування Socket.IO подій

## Проблема

З логів бачимо:
- ✅ Socket підключений
- ✅ Повідомлення відправлені
- ❌ Повідомлення НЕ отримуються назад

## Можливі причини:

### 1. Проблема з `join` event (найімовірніше!)

**Що відправляє клієнт:**
```kotlin
val authData = JSONObject().apply {
    put("user_id", UserSession.accessToken)  // ← ЦЕ access_token (хеш)!
}
socket?.emit("join", authData)
```

**Проблема:** WoWonder очікує `access_token` як **session hash**, а НЕ числовий ID!

**Як сервер має обробляти:**
```javascript
socket.on('join', function(data) {
    // data.user_id це access_token (хеш типу "d00d1617c8...")

    // Знайти числовий user_id з бази даних
    db.query(
        'SELECT user_id FROM wo_users WHERE access_token = ?',
        [data.user_id],
        function(err, results) {
            if (results && results.length > 0) {
                const numericUserId = results[0].user_id;

                // Приєднати до room з числовим ID
                socket.join('user_' + numericUserId);

                console.log('User', numericUserId, 'joined with token', data.user_id.substring(0, 10));
            }
        }
    );
});
```

### 2. Неправильний формат події від сервера

**Клієнт слухає:**
- `private_message`
- `new_message`
- `private_message_page`
- `page_message`

**Сервер може емітити:**
- `private_message` ✅
- `new_message` ✅
- `message` ❌ (не слухається!)
- `user_message` ❌ (не слухається!)

### 3. Події емітяться не в той room

**Правильно:**
```javascript
// До отримувача
io.to('user_' + toUserId).emit('private_message', data);

// До відправника також (щоб побачив своє повідомлення)
io.to('user_' + fromUserId).emit('private_message', data);
```

**Неправильно:**
```javascript
// До всіх (не використовується в 1-на-1 чатах)
io.emit('private_message', data);

// До конкретного socket (втратиться при переключенні екранів)
socket.emit('private_message', data);
```

## Тимчасовий патч для діагностики

### На клієнті: Слухайте ВСІ події

Додайте в `SocketManager.kt` в `connect()`:

```kotlin
// ТИМЧАСОВО: Слухаємо ВСІ можливі події для діагностики
val possibleEvents = arrayOf(
    "message",
    "new_message",
    "private_message",
    "private_message_page",
    "page_message",
    "user_message",
    "chat_message",
    "receive_message"
)

possibleEvents.forEach { eventName ->
    socket?.on(eventName) { args ->
        Log.w("SocketManager", "🎯 CAUGHT event '$eventName' with ${args.size} args: ${args.firstOrNull()}")

        if (args.isNotEmpty() && args[0] is JSONObject) {
            listener.onNewMessage(args[0] as JSONObject)
        }
    }
}
```

Це допоможе побачити **яку саме подію** відправляє сервер.

### На сервері: Логування всіх emit

В `events.js` додайте wrapper:

```javascript
const originalEmit = io.emit;
io.emit = function(event, ...args) {
    console.log('📤 Socket.IO EMIT:', event, 'to all clients');
    return originalEmit.apply(this, [event, ...args]);
};

// І для to()
const originalTo = io.to;
io.to = function(room) {
    const result = originalTo.apply(this, arguments);
    const originalRoomEmit = result.emit;

    result.emit = function(event, ...args) {
        console.log('📤 Socket.IO EMIT:', event, 'to room', room);
        return originalRoomEmit.apply(this, [event, ...args]);
    };

    return result;
};
```

## Що перевірити ЗАРАЗ:

### 1. Перевірте логи сервера

Коли ви надсилаєте повідомлення з додатка, на сервері має з'явитись:

```
🔥 RECEIVED private_message from client: { from_id: 8, to_id: X, ... }
📤 Socket.IO EMIT: private_message to room user_X
✅ Message emitted successfully
```

Якщо **НЕ з'являється** - проблема в обробнику `private_message` на сервері.

### 2. Перевірте чи користувач в room

В логах Node.js має бути:

```
👤 User joining: { user_id: 'd00d1617c8...', socket_id: 'abc123' }
✅ User joined room: user_8
```

Якщо **НЕ з'являється** - проблема в обробнику `join`.

### 3. Тест через browser console

Відкрийте веб-версію мессенджера (якщо є) і в консолі браузера:

```javascript
// Підключитись до Socket.IO
const socket = io('https://worldmates.club:449', {
    query: 'access_token=YOUR_TOKEN&user_id=8'
});

// Аутентифікація
socket.emit('join', { user_id: 'YOUR_ACCESS_TOKEN' });

// Відправити тест
socket.emit('private_message', {
    from_id: 8,
    to_id: 8,  // Самому собі
    msg: 'TEST'
});

// Слухати відповідь
socket.on('private_message', (data) => {
    console.log('RECEIVED:', data);
});
```

Якщо **спрацює в браузері** але НЕ в додатку - проблема в Android клієнті.
Якщо **НЕ спрацює** - проблема на сервері.

## Швидке виправлення

Якщо сервер використовує старий формат WoWonder, можливо потрібно:

### В listeners.js змінити:

```javascript
// СТАРИЙ КОД (не працює):
socket.on('private_message', function(data) {
    // Зберігає в БД
    saveMessage(data);
    // ❌ НЕ емітить назад!
});

// НОВИЙ КОД (працює):
socket.on('private_message', function(data) {
    // Зберігає в БД
    saveMessage(data, function(savedMessage) {
        // ✅ Емітить назад після збереження
        io.to('user_' + data.to_id).emit('private_message', savedMessage);
        io.to('user_' + data.from_id).emit('private_message', savedMessage);
    });
});
```

---

**Створено:** 2025-12-26
**Автор:** Claude Code Agent

## Наступні кроки:

1. ✅ Додайте патч для слухання всіх подій (тимчасово)
2. ✅ Надішліть повідомлення в чаті
3. ✅ Перевірте логи - яку подію ви "зловили"
4. ✅ Надішліть мені назву події
5. Я допоможу налаштувати сервер правильно
