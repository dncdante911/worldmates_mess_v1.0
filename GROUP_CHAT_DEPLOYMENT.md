# Інструкції з розгортання Group Chat API

## Крок 1: Застосування міграції бази даних

Підключіться до вашого сервера MySQL/MariaDB та виконайте міграцію:

```bash
# Опція 1: Через командний рядок
mysql -u social -p socialhub < migrations/001_extend_group_chat.sql

# Опція 2: Через MySQL клієнт
mysql -u social -p
USE socialhub;
SOURCE /path/to/modejs-mess/migrations/001_extend_group_chat.sql;
```

**Примітка:** Міграція безпечна для повторного запуску - вона перевіряє існування колонок перед додаванням.

---

## Крок 2: Оновлення файлів Node.js сервера

Скопіюйте наступні файли на ваш сервер:

### Нові контролери (controllers/):
1. `CreateGroupController.js`
2. `UpdateGroupController.js`
3. `DeleteGroupController.js`
4. `AddGroupMemberController.js`
5. `RemoveGroupMemberController.js`
6. `SetGroupRoleController.js`
7. `LeaveGroupController.js`
8. `GetGroupDetailsController.js`

### Оновлені файли:
1. `models/wo_groupchat.js` - додано нові поля
2. `models/wo_groupchatusers.js` - додано роль
3. `listeners/listeners.js` - зареєстровано нові events

---

## Крок 3: Перезапуск Node.js сервера

```bash
# Якщо використовуєте PM2
pm2 restart socketioserver

# Або якщо використовуєте systemd
systemctl restart socketioserver

# Або просто вбити процес і запустити знову
pkill -f "node main.js"
cd /path/to/modejs-mess
node main.js

# Або якщо використовуєте npm start
npm start
```

---

## Крок 4: Перевірка

Перевірте логи сервера, щоб переконатися що все працює:

```bash
# Якщо PM2
pm2 logs socketioserver

# Або просто дивіться console output
```

Має з'явитися повідомлення про успішне підключення без помилок.

---

## Крок 5: Тестування API

Використайте WebSocket клієнт для тестування. Приклад на JavaScript:

```javascript
const io = require('socket.io-client');

const socket = io('https://worldmates.club', {
  transports: ['websocket'],
  query: {
    access_token: 'YOUR_ACCESS_TOKEN'
  }
});

socket.on('connect', () => {
  console.log('Connected!');

  // Test: Create group
  socket.emit('create_group', {
    from_id: 'YOUR_USER_HASH',
    group_name: 'Test Group',
    description: 'Testing group creation'
  }, (response) => {
    console.log('Create group response:', response);

    if (response.status === 200) {
      const groupId = response.group.group_id;

      // Test: Get group details
      socket.emit('get_group_details', {
        from_id: 'YOUR_USER_HASH',
        group_id: groupId
      }, (detailsResponse) => {
        console.log('Group details:', detailsResponse);
      });
    }
  });
});

socket.on('group_created', (data) => {
  console.log('Group created event:', data);
});
```

---

## Крок 6: Інтеграція в Android додаток

Після успішного тестування серверної частини, можна починати інтеграцію в Android:

1. ✅ Серверна частина готова
2. ⏳ Потрібно створити UI для створення груп
3. ⏳ Потрібно створити екран деталей групи
4. ⏳ Потрібно інтегрувати управління учасниками

---

## Структура проекту після оновлення

```
modejs-mess/
├── migrations/
│   └── 001_extend_group_chat.sql       ← НОВЕ
├── models/
│   ├── wo_groupchat.js                 ← ОНОВЛЕНО
│   └── wo_groupchatusers.js            ← ОНОВЛЕНО
├── controllers/
│   ├── CreateGroupController.js        ← НОВЕ
│   ├── UpdateGroupController.js        ← НОВЕ
│   ├── DeleteGroupController.js        ← НОВЕ
│   ├── AddGroupMemberController.js     ← НОВЕ
│   ├── RemoveGroupMemberController.js  ← НОВЕ
│   ├── SetGroupRoleController.js       ← НОВЕ
│   ├── LeaveGroupController.js         ← НОВЕ
│   ├── GetGroupDetailsController.js    ← НОВЕ
│   └── ... (існуючі контролери)
├── listeners/
│   └── listeners.js                    ← ОНОВЛЕНО
├── GROUP_CHAT_API.md                   ← НОВЕ (документація)
└── DEPLOYMENT_INSTRUCTIONS.md          ← НОВЕ (цей файл)
```

---

## Можливі проблеми та рішення

### Проблема: Помилка "Cannot find module ..."

**Рішення:** Перевірте що всі контролери скопійовано в правильну директорію.

### Проблема: SQL помилка при виконанні міграції

**Рішення:** Перевірте версію MariaDB (має бути 10.11+) та права доступу користувача.

### Проблема: Socket.IO events не спрацьовують

**Рішення:**
1. Перевірте що сервер перезапущено
2. Перевірте логи на наявність помилок
3. Перевірте що `from_id` відповідає хешу користувача

---

## Наступні кроки

1. ✅ Сервер готовий
2. ⏳ Потрібно створити Android UI
3. ⏳ Потрібно інтегрувати Socket.IO події в SocketManager.kt
4. ⏳ Потрібно оновити GroupsViewModel для використання нових API

---

## Підтримка

Якщо виникли проблеми:
1. Перевірте логи Node.js сервера
2. Перевірте логи MySQL
3. Перевірте що міграція виконалась успішно: `SHOW COLUMNS FROM Wo_GroupChat;`
