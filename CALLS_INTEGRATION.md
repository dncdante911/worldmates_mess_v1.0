# Интеграция WebRTC звонков в WorldMates Messenger

## 📋 Обзор

Это руководство поможет интегрировать функционал аудио/видео звонков в ваш существующий Node.js проект на `/var/www/www-root/data/www/worldmates.club/nodejs/`

## 🗂️ Структура файлов

```
worldmates_mess_v1.0/
├── nodejs-models/               # Модели Sequelize (скопировать на сервер)
│   ├── wo_calls.js
│   ├── wo_group_calls.js
│   ├── wo_group_call_participants.js
│   ├── wo_ice_candidates.js
│   └── wo_call_statistics.js
├── nodejs-integration/          # Listener для интеграции
│   └── calls-listener.js
└── create-calls-tables.sql     # SQL для создания таблиц
```

## 📥 Шаг 1: Создать таблицы в базе данных

Выполните SQL файл на вашей MariaDB 10.11.13:

```bash
mysql -u social -p socialhub < create-calls-tables.sql
```

Это создаст следующие таблицы:
- `wo_calls` - 1-на-1 звонки
- `wo_group_calls` - групповые звонки
- `wo_group_call_participants` - участники групповых звонков
- `wo_ice_candidates` - ICE candidates для WebRTC
- `wo_call_statistics` - статистика качества звонков

## 📂 Шаг 2: Скопировать модели на сервер

Скопируйте все файлы из `nodejs-models/` в папку `models/` вашего Node.js проекта:

```bash
# На вашем сервере
cd /var/www/www-root/data/www/worldmates.club/nodejs/models/

# Скопируйте эти 5 файлов:
# - wo_calls.js
# - wo_group_calls.js
# - wo_group_call_participants.js
# - wo_ice_candidates.js
# - wo_call_statistics.js
```

## 📂 Шаг 3: Скопировать listener

Скопируйте `nodejs-integration/calls-listener.js` в папку `listeners/`:

```bash
cd /var/www/www-root/data/www/worldmates.club/nodejs/listeners/
# Скопировать calls-listener.js сюда
```

## ⚙️ Шаг 4: Обновить main.js

Добавьте загрузку новых моделей в функцию `init()` в вашем `main.js`:

```javascript
async function init() {
  var sequelize = new Sequelize(/* ... ваша конфигурация ... */);

  // ========== СУЩЕСТВУЮЩИЕ МОДЕЛИ ==========
  ctx.wo_messages = require("./models/wo_messages")(sequelize, DataTypes)
  ctx.wo_userschat = require("./models/wo_userschat")(sequelize, DataTypes)
  // ... остальные существующие модели ...

  // ========== НОВЫЕ МОДЕЛИ ДЛЯ ЗВОНКОВ ==========
  ctx.wo_calls = require("./models/wo_calls")(sequelize, DataTypes)
  ctx.wo_group_calls = require("./models/wo_group_calls")(sequelize, DataTypes)
  ctx.wo_group_call_participants = require("./models/wo_group_call_participants")(sequelize, DataTypes)
  ctx.wo_ice_candidates = require("./models/wo_ice_candidates")(sequelize, DataTypes)
  ctx.wo_call_statistics = require("./models/wo_call_statistics")(sequelize, DataTypes)

  // ... остальной код ...
}
```

## 🔗 Шаг 5: Зарегистрировать listener

### Вариант A: Интеграция в существующий `listeners/listeners.js`

Если у вас есть файл `listeners/listeners.js` с функцией `registerListeners`, добавьте туда вызов:

```javascript
// listeners/listeners.js

const registerCallsListeners = require('./calls-listener');
// ... другие импорты ...

async function registerListeners(socket, io, ctx) {
    // ========== СУЩЕСТВУЮЩИЕ LISTENERS ==========
    // ваши существующие обработчики...

    // ========== НОВЫЙ LISTENER ДЛЯ ЗВОНКОВ ==========
    await registerCallsListeners(socket, io, ctx);
}

module.exports = { registerListeners };
```

### Вариант B: Прямая регистрация в main.js

Если у вас нет модульной системы listeners, добавьте в `main.js`:

```javascript
const registerCallsListeners = require('./listeners/calls-listener');

// В функции main(), в обработчике io.on('connection')
io.on('connection', async (socket, query) => {
    // Ваши существующие listeners
    await listeners.registerListeners(socket, io, ctx);

    // Добавить регистрацию звонков
    await registerCallsListeners(socket, io, ctx);
});
```

## 🔄 Шаг 6: Перезапустить Node.js сервер

```bash
# Если используете PM2
pm2 restart your-app-name

# Или если используете nodemon
npm start

# Или просто Node.js
node main.js
```

## 📱 Шаг 7: Проверить работу

Проверьте в логах:

```bash
tail -f /path/to/your/logs

# Вы должны увидеть:
# [CALLS] Call listeners registered for socket xyz123
```

## 🧪 Тестирование

### События Socket.IO для звонков

#### 1. Регистрация пользователя для звонков

```javascript
// Android приложение
socket.emit('call:register', {
    userId: 123
});
```

#### 2. Инициация 1-на-1 звонка

```javascript
socket.emit('call:initiate', {
    fromId: 123,
    toId: 456,
    callType: 'video',  // или 'audio'
    roomName: 'room_123_456_1638360000',
    sdpOffer: { /* SDP offer объект */ }
});

// Получатель получит:
socket.on('call:incoming', (data) => {
    console.log(data.fromName);      // "John Doe"
    console.log(data.callType);      // "video"
    console.log(data.roomName);      // "room_123_456_1638360000"
    console.log(data.sdpOffer);      // SDP offer
});
```

#### 3. Принять звонок

```javascript
socket.emit('call:accept', {
    roomName: 'room_123_456_1638360000',
    userId: 456,
    sdpAnswer: { /* SDP answer объект */ }
});

// Инициатор получит:
socket.on('call:answer', (data) => {
    console.log(data.sdpAnswer);  // SDP answer
});
```

#### 4. Обмен ICE candidates

```javascript
socket.emit('ice:candidate', {
    roomName: 'room_123_456_1638360000',
    toUserId: 456,
    fromUserId: 123,
    candidate: { /* ICE candidate */ },
    sdpMLineIndex: 0,
    sdpMid: 'audio'
});

socket.on('ice:candidate', (data) => {
    // Добавить candidate в RTCPeerConnection
});
```

#### 5. Завершить звонок

```javascript
socket.emit('call:end', {
    roomName: 'room_123_456_1638360000',
    userId: 123,
    reason: 'completed'  // или 'cancelled', 'failed'
});

// Другой участник получит:
socket.on('call:ended', (data) => {
    console.log(data.reason);   // "completed"
    console.log(data.endedBy);  // 123
});
```

#### 6. Отклонить звонок

```javascript
socket.emit('call:reject', {
    roomName: 'room_123_456_1638360000',
    userId: 456
});

// Инициатор получит:
socket.on('call:rejected', (data) => {
    console.log('Call was rejected');
});
```

## 🎯 Групповые звонки

#### Инициация группового звонка

```javascript
socket.emit('call:initiate', {
    fromId: 123,
    groupId: 789,           // ID группы вместо toId
    callType: 'video',
    roomName: 'group_789_1638360000',
    sdpOffer: { /* SDP offer */ }
});

// Все члены группы получат:
socket.on('group_call:incoming', (data) => {
    console.log(data.groupId);       // 789
    console.log(data.initiatorName); // "John Doe"
    console.log(data.callType);      // "video"
});
```

#### Присоединиться к комнате

```javascript
socket.emit('call:join_room', {
    roomName: 'group_789_1638360000',
    userId: 456
});

// Другие участники получат:
socket.on('user:joined_call', (data) => {
    console.log(data.userId + ' joined');
});
```

#### Переключение медиа

```javascript
socket.emit('call:toggle_media', {
    roomName: 'group_789_1638360000',
    userId: 456,
    audio: false,  // отключил микрофон
    video: true    // камера включена
});

// Другие участники получат:
socket.on('user:media_changed', (data) => {
    console.log(`User ${data.userId}: audio=${data.audio}, video=${data.video}`);
});
```

## 🔍 Структура базы данных

### wo_calls (1-на-1 звонки)

```sql
SELECT * FROM wo_calls WHERE from_id = 123;
```

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | INT | Primary key |
| from_id | INT | Инициатор звонка |
| to_id | INT | Получатель |
| call_type | ENUM | 'audio' или 'video' |
| status | ENUM | 'ringing', 'connected', 'ended', 'missed', 'rejected' |
| room_name | VARCHAR(100) | Уникальное имя комнаты |
| created_at | DATETIME | Время создания |
| accepted_at | DATETIME | Время принятия |
| ended_at | DATETIME | Время завершения |
| duration | INT | Длительность в секундах |

### wo_group_calls (Групповые звонки)

```sql
SELECT * FROM wo_group_calls WHERE group_id = 789;
```

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | INT | Primary key |
| group_id | INT | ID группы |
| initiated_by | INT | Кто начал |
| call_type | ENUM | 'audio' или 'video' |
| status | ENUM | 'ringing', 'active', 'ended' |
| room_name | VARCHAR(100) | Имя комнаты |

## 🐛 Отладка

### Проверить подключение к БД

```javascript
// В main.js после init()
console.log('Testing calls model:');
const testCall = await ctx.wo_calls.findOne({ limit: 1 });
console.log('Call model OK:', testCall);
```

### Логи событий Socket.IO

Все события звонков логируются с префиксом `[CALLS]`:

```bash
[CALLS] User registered for calls: 123
[CALLS] Call initiated: 123 -> 456 (video)
[CALLS] Incoming call sent to user 456 (2 devices)
[CALLS] Call accepted: room_123_456_1638360000 by user 456
[CALLS] Answer sent to initiator 123
[CALLS] Call ended: room_123_456_1638360000 by 123 (completed)
```

### Проверить активные Socket.IO соединения

```javascript
// В Node.js консоли
console.log('Active sockets:', Object.keys(ctx.userIdSocket));
console.log('Active calls:', ctx.activeCalls.size);
```

## ⚠️ Важные моменты

### 1. Совместимость с существующей системой

Listener использует:
- ✅ `ctx.userIdSocket` - массив сокетов для каждого пользователя
- ✅ Sequelize модели из `ctx.wo_*`
- ✅ Существующий паттерн `registerListeners(socket, io, ctx)`
- ✅ Отправка на все устройства пользователя (multi-device support)

### 2. Redis (если используется)

Если у вас включен Redis (`ctx.globalconfig["redis"] === "Y"`), Socket.IO уже настроен в main.js и будет работать с масштабированием.

### 3. Push уведомления (опционально)

Вы можете добавить Firebase Cloud Messaging для push уведомлений о пропущенных звонках:

```javascript
// В calls-listener.js после строки:
// await ctx.wo_calls.update({ status: 'missed' }, ...)

// Добавить:
await sendPushNotification(toId, {
    title: `Missed call from ${initiator.first_name}`,
    body: `${callType} call`,
    data: {
        type: 'missed_call',
        fromId: fromId,
        roomName: roomName
    }
});
```

### 4. TURN/STUN сервера

Для работы WebRTC нужны TURN/STUN сервера. Конфигурация находится в `turnserver.conf`.

Настройте TURN сервер (Coturn):

```bash
apt-get install coturn
cp turnserver.conf /etc/turnserver.conf
systemctl start coturn
```

В Android приложении (WebRTCManager.kt):

```kotlin
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:worldmates.club:3478").createIceServer(),
    PeerConnection.IceServer.builder("turn:worldmates.club:3478")
        .setUsername("wmuser")
        .setPassword("wmpass")
        .createIceServer()
)
```

## 📊 Мониторинг

### Получить статистику звонков

```sql
-- Всего звонков за сегодня
SELECT COUNT(*) FROM wo_calls
WHERE DATE(created_at) = CURDATE();

-- Успешные звонки (были приняты)
SELECT COUNT(*) FROM wo_calls
WHERE status = 'connected' AND DATE(created_at) = CURDATE();

-- Средняя длительность звонков
SELECT AVG(duration) as avg_duration
FROM wo_calls
WHERE duration IS NOT NULL;

-- Самые активные пользователи
SELECT from_id, COUNT(*) as calls_count
FROM wo_calls
GROUP BY from_id
ORDER BY calls_count DESC
LIMIT 10;
```

## 🆘 Частые проблемы

### Проблема: "Cannot read property 'wo_calls' of undefined"

**Решение**: Убедитесь, что модели загружены в main.js в функции `init()`.

### Проблема: "Recipient is offline" хотя пользователь онлайн

**Решение**: Убедитесь, что Android приложение отправляет `call:register` при подключении:

```kotlin
// В SocketManager.kt
socket.emit("call:register", JSONObject().apply {
    put("userId", userId)
})
```

### Проблема: ICE candidates не доходят

**Решение**: Проверьте, что пользователи присоединились к комнате:

```javascript
socket.emit('call:join_room', { roomName: roomName, userId: userId });
```

### Проблема: Групповой звонок не работает

**Решение**: Убедитесь, что таблица `wo_groupchatusers` существует и содержит участников группы.

## 📞 Поддержка

Если возникли проблемы:
1. Проверьте логи Node.js сервера
2. Убедитесь, что все таблицы созданы
3. Проверьте, что модели загружены в `ctx`
4. Проверьте Socket.IO подключение в Android приложении

---

**Готово!** 🎉 Ваш WorldMates Messenger теперь поддерживает аудио/видео звонки!
