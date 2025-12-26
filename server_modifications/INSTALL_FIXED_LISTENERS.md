# 🔧 ВИПРАВЛЕННЯ listeners.js - КРИТИЧНІ ПОМИЛКИ

## ❌ Знайдені проблеми:

### 1. Групові обробники всередині `disconnect` (КРИТИЧНО!)

**Проблема:**
```javascript
socket.on('disconnect', async (reason) => {
    DisconnectController(ctx, reason, io,socket);

    // ❌ ПОМИЛКА: Всі ці обробники ВСЕРЕДИНІ disconnect!
    socket.on('create_group', async (data, callback) => {
        ...
    })
    // ... інші обробники
})
```

**Наслідок:** Обробники груп **НІКОЛИ НЕ ВИКОНУЮТЬСЯ**, бо вони реєструються тільки після disconnect!

**Виправлення:** Винесено всі групові обробники з `disconnect`.

### 2. Недостатньо логування

Додано діагностичне логування для:
- `join` event
- `private_message` event
- `typing` event
- `disconnect` event

### 3. Redis емітить тільки в один room

**Проблема:** Redis емітить тільки до `to_id`, але НЕ до `from_id`

**Наслідок:** Відправник не бачить своє повідомлення в real-time!

**Виправлення:**
```javascript
// До отримувача
io.to(targetUserId).emit('new_message', msgData);
io.to(targetUserId).emit('private_message', msgData);

// ✅ ДОДАТИ: До відправника також!
io.to(String(decoded.from_id)).emit('new_message', msgData);
io.to(String(decoded.from_id)).emit('private_message', msgData);
```

## 📋 Інструкції з встановлення:

### Крок 1: Backup старого файлу

```bash
# На сервері
cd /path/to/nodejs/listeners
cp listeners.js listeners.js.backup
```

### Крок 2: Замініть listeners.js

Скопіюйте вміст з `server_modifications/nodejs_listeners_FIXED.js` в `nodejs/listeners/listeners.js`

```bash
# Якщо є доступ до репозиторію
scp server_modifications/nodejs_listeners_FIXED.js server:/path/to/nodejs/listeners/listeners.js
```

### Крок 3: Перезапустіть Node.js

```bash
# Якщо використовується PM2
pm2 restart all

# Або вручну
killall node
cd /path/to/nodejs
node main.js &
```

### Крок 4: Перевірте логи

```bash
pm2 logs

# Шукайте:
# 🔌 User connected
# 🔥 JOIN event received
# 🔥 PRIVATE_MESSAGE event received
# >>> Emitted new_message to room
```

## 🔍 Додаткова перевірка JoinController

Також потрібно перевірити `JoinController.js`:

```javascript
// controllers/JoinController.js

async function JoinController(ctx, data, io, socket, callback) {
    console.log("🔥 JoinController: Processing join for:", data);

    try {
        // data.user_id це access_token (хеш)
        const accessToken = data.user_id;

        // КРИТИЧНО: Знайти числовий user_id з бази даних
        const query = `SELECT user_id FROM ${ctx.config.prefix}_users WHERE access_token = ?`;
        const [results] = await ctx.conn.promise().query(query, [accessToken]);

        if (results && results.length > 0) {
            const numericUserId = results[0].user_id;
            const roomName = String(numericUserId);

            // Додати socket до room з числовим ID
            socket.join(roomName);

            console.log(`✅ User ${numericUserId} joined room: ${roomName}`);
            console.log(`   Socket ID: ${socket.id}`);
            console.log(`   Access Token: ${accessToken.substring(0, 10)}...`);

            // Зберегти для подальшого використання
            socket.userId = numericUserId;
            socket.accessToken = accessToken;

            // Callback якщо потрібно
            if (callback) {
                callback({ status: 200, user_id: numericUserId });
            }
        } else {
            console.log(`❌ User NOT FOUND for access_token: ${accessToken.substring(0, 10)}...`);
            if (callback) {
                callback({ status: 404, error: 'User not found' });
            }
        }
    } catch (error) {
        console.log("❌ JoinController error:", error.message);
        if (callback) {
            callback({ status: 500, error: error.message });
        }
    }
}

module.exports = { JoinController };
```

## 🧪 Тестування після виправлень:

### 1. Перевірте підключення

```bash
# Логи мають показувати:
🔌 User connected: socket_id=abc123 query={"access_token":"...","user_id":"8"}
🔥 JOIN event received: {user_id: "d00d1617c8..."}
✅ User 8 joined room: 8
```

### 2. Надішліть повідомлення

```bash
# Логи мають показувати:
🔥 PRIVATE_MESSAGE event received: {from_id: 8, to_id: 24, msg: "test"}
=== Redis: Получено сообщение для user_24 ===
>>> Emitted new_message to room: 24
>>> Emitted private_message to room: 24
✅ Redis: Всі емити виконані успішно
```

### 3. Перевірте на клієнті

В `adb logcat` має з'явитись:

```
SocketManager: 📨 private_message event received with 1 args
SocketManager: ✅ private_message JSON: {id:123, from_id:8, to_id:24, text:"...", time:...}
MessagesViewModel: 📨 Отримано Socket.IO повідомлення
MessagesViewModel: Додано нове повідомлення від Socket.IO
```

## ⚠️ Якщо все ще не працює:

### Можлива проблема: PrivateMessageController не публікує в Redis

Перевірте `controllers/PrivateMessageController.js`:

```javascript
// Після збереження повідомлення в БД:

// Публікація в Redis для real-time доставки
const redisMessage = JSON.stringify({
    to_id: data.to_id,
    from_id: data.from_id,
    data: {
        id: savedMessage.id,
        from_id: data.from_id,
        to_id: data.to_id,
        text: data.msg,
        time: Math.floor(Date.now() / 1000),
        media: savedMessage.media || '',
        // ... інші поля
    }
});

// Публікація в канал messages
await redisPublisher.publish('messages', redisMessage);
console.log(`📤 Published to Redis channel 'messages' for user ${data.to_id}`);
```

Якщо цього коду **НЕМАЄ** - повідомлення не будуть доставлятися в real-time!

---

**Створено:** 2025-12-26
**Автор:** Claude Code Agent
**Статус:** Готово до встановлення
