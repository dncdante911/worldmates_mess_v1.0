# 📞 Интеграция звонков - Краткая инструкция

## 🚀 Быстрый старт

### 1️⃣ Создать таблицы в БД

```bash
mysql -u social -p socialhub < create-calls-tables.sql
```

### 2️⃣ Скопировать файлы на сервер

**Модели** (5 файлов из `nodejs-models/`) → `/var/www/www-root/data/www/worldmates.club/nodejs/models/`:
- ✅ wo_calls.js
- ✅ wo_group_calls.js
- ✅ wo_group_call_participants.js
- ✅ wo_ice_candidates.js
- ✅ wo_call_statistics.js

**Listener** (1 файл из `nodejs-integration/`) → `/var/www/www-root/data/www/worldmates.club/nodejs/listeners/`:
- ✅ calls-listener.js

### 3️⃣ Обновить main.js

Добавить в функцию `init()` после существующих моделей:

```javascript
// НОВЫЕ МОДЕЛИ ДЛЯ ЗВОНКОВ
ctx.wo_calls = require("./models/wo_calls")(sequelize, DataTypes)
ctx.wo_group_calls = require("./models/wo_group_calls")(sequelize, DataTypes)
ctx.wo_group_call_participants = require("./models/wo_group_call_participants")(sequelize, DataTypes)
ctx.wo_ice_candidates = require("./models/wo_ice_candidates")(sequelize, DataTypes)
ctx.wo_call_statistics = require("./models/wo_call_statistics")(sequelize, DataTypes)
```

### 4️⃣ Зарегистрировать listener

В файле `listeners/listeners.js` добавить:

```javascript
const registerCallsListeners = require('./calls-listener');

async function registerListeners(socket, io, ctx) {
    // ... ваши существующие listeners ...

    // ДОБАВИТЬ ЭТУ СТРОКУ:
    await registerCallsListeners(socket, io, ctx);
}
```

### 5️⃣ Перезапустить сервер

```bash
pm2 restart your-app
# или
npm start
```

## ✅ Проверка

В логах должно появиться:
```
[CALLS] Call listeners registered for socket xyz123
```

## 📱 Тестирование из Android

```kotlin
// 1. Регистрация
socket.emit("call:register", JSONObject().apply {
    put("userId", currentUserId)
})

// 2. Начать звонок
socket.emit("call:initiate", JSONObject().apply {
    put("fromId", fromUserId)
    put("toId", toUserId)
    put("callType", "video")
    put("roomName", "room_${fromId}_${toId}_${System.currentTimeMillis()}")
    put("sdpOffer", sdpOfferJson)
})
```

## 📚 Полная документация

Смотрите **CALLS_INTEGRATION.md** для:
- Подробных примеров всех событий
- Групповых звонков
- Отладки
- Решения проблем
- Мониторинга

## 🎯 События Socket.IO

| Событие | Кто отправляет | Описание |
|---------|----------------|----------|
| `call:register` | Клиент | Регистрация для звонков |
| `call:initiate` | Клиент | Начать звонок |
| `call:incoming` | Сервер → Клиент | Входящий звонок |
| `call:accept` | Клиент | Принять звонок |
| `call:answer` | Сервер → Клиент | SDP answer |
| `call:end` | Клиент | Завершить звонок |
| `call:ended` | Сервер → Клиент | Звонок завершен |
| `call:reject` | Клиент | Отклонить звонок |
| `call:rejected` | Сервер → Клиент | Звонок отклонен |
| `ice:candidate` | Клиент ↔ Клиент | ICE candidates |
| `call:join_room` | Клиент | Присоединиться к комнате |
| `call:leave_room` | Клиент | Покинуть комнату |

## 🔧 Структура файлов проекта

```
/var/www/www-root/data/www/worldmates.club/nodejs/
│
├── main.js                          (обновить - добавить загрузку моделей)
├── config.json                      (без изменений)
├── package.json                     (без изменений)
│
├── models/
│   ├── wo_messages.js              (существующие)
│   ├── wo_users.js                 (существующие)
│   ├── ...                         (существующие)
│   ├── wo_calls.js                 ← НОВЫЙ
│   ├── wo_group_calls.js           ← НОВЫЙ
│   ├── wo_group_call_participants.js ← НОВЫЙ
│   ├── wo_ice_candidates.js        ← НОВЫЙ
│   └── wo_call_statistics.js       ← НОВЫЙ
│
└── listeners/
    ├── listeners.js                (обновить - добавить вызов)
    └── calls-listener.js           ← НОВЫЙ
```

## ❗ Важно

1. **Не забудьте** выполнить SQL для создания таблиц
2. **Обязательно** добавьте модели в `main.js`
3. **Перезапустите** Node.js сервер после изменений
4. **Проверьте** логи на наличие ошибок

## 🆘 Проблемы?

### Ошибка: "wo_calls is not defined"
→ Не загружены модели в main.js

### Ошибка: "Table 'socialhub.wo_calls' doesn't exist"
→ Не выполнен SQL файл

### Звонки не доходят
→ Проверьте `call:register` в Android приложении

---

**Готово!** 🎉 Если все сделано правильно, звонки должны работать.
