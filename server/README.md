# 📞 WorldMates Video Calls Backend

Backend сервер для підтримки WebRTC відеодзвінків у WorldMates Messenger.

---

## 🚀 Швидкий старт (3 кроки!)

### 1️⃣ Встановити coturn:

```bash
sudo apt-get update
sudo apt-get install coturn -y
```

### 2️⃣ Налаштувати TURN сервер:

```bash
# Скопіювати конфігурацію
sudo cp turnserver.conf /etc/turnserver.conf

# Отримати ваш публічний IP
curl ifconfig.me

# Встановити IP у конфіг
sudo nano /etc/turnserver.conf
# Знайти: external-ip=YOUR_PUBLIC_IP_HERE
# Замінити на ваш IP

# Увімкнути coturn
sudo nano /etc/default/coturn
# Розкоментувати: TURNSERVER_ENABLED=1
```

### 3️⃣ Запустити все одною командою:

```bash
cd server
./start-all.sh
```

**Готово! 🎉**

---

## 📁 Структура файлів

```
server/
├── turnserver.conf              # Конфігурація TURN сервера (coturn)
├── generate-turn-credentials.js # Генерація TURN credentials
├── callsSocketHandler.js        # Socket.IO обробники для дзвінків
├── server-example.js            # Приклад Node.js сервера
├── start-all.sh                 # Скрипт запуску всього
├── package.json                 # NPM dependencies
├── TURN_SETUP_INSTRUCTIONS.md   # Детальна інструкція
└── README.md                    # Цей файл
```

---

## 🔧 Конфігурація

### turnserver.conf - Основні параметри:

```ini
listening-port=3478              # TURN порт (UDP/TCP)
tls-listening-port=5349          # TLS TURN порт
realm=worldmates.club            # Ваш домен
external-ip=YOUR_PUBLIC_IP       # ⚠️ ОБОВ'ЯЗКОВО замінити!
static-auth-secret=a7f3e9...     # Секретний ключ (вже згенерований)
cert=/var/www/.../cert.crt       # SSL сертифікат
pkey=/var/www/.../cert.key       # SSL приватний ключ
```

### Порти що потрібно відкрити:

```bash
# TURN порти
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp

# Relay порти
sudo ufw allow 49152:65535/udp
sudo ufw allow 49152:65535/tcp

# Node.js порт
sudo ufw allow 449/tcp

# Reload
sudo ufw reload
```

---

## 📡 API Endpoints

### GET /api/health

Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "timestamp": "2024-01-17T10:30:00.000Z",
  "uptime": 12345.67
}
```

### GET /api/ice-servers/:userId

Отримати ICE configuration з динамічними TURN credentials.

**Example:**
```bash
curl http://localhost:449/api/ice-servers/123
```

**Response:**
```json
{
  "success": true,
  "iceServers": [
    { "urls": "stun:stun.l.google.com:19302" },
    {
      "urls": ["turn:worldmates.club:3478?transport=udp"],
      "username": "1705488600:123",
      "credential": "base64encodedpassword"
    }
  ]
}
```

### POST /api/turn-credentials

Згенерувати TURN credentials (альтернативний метод).

**Request:**
```json
{
  "userId": "123",
  "ttl": 86400
}
```

### GET /api/admin/calls/stats

Статистика активних дзвінків (тільки для адміністраторів).

**Response:**
```json
{
  "success": true,
  "stats": {
    "totalCalls": 5,
    "calls": [
      {
        "roomName": "room_1705488600_abc",
        "participants": [123, 456],
        "callType": "video",
        "duration": 120
      }
    ]
  }
}
```

---

## 🔌 Socket.IO Events

### Client → Server

| Event | Payload | Description |
|-------|---------|-------------|
| `user:join` | `userId` | Приєднатися до особистої кімнати |
| `call:initiate` | `{ fromId, toId, callType, roomName, sdpOffer }` | Ініціювати дзвінок |
| `call:accept` | `{ roomName, fromId, sdpAnswer }` | Прийняти дзвінок |
| `call:reject` | `{ roomName }` | Відхилити дзвінок |
| `ice:candidate` | `{ roomName, candidate, sdpMLineIndex, sdpMid }` | Відправити ICE candidate |
| `call:end` | `{ roomName, reason }` | Завершити дзвінок |
| `group_call:initiate` | `{ groupId, initiatedBy, callType, roomName, memberIds }` | Груповий дзвінок |

### Server → Client

| Event | Payload | Description |
|-------|---------|-------------|
| `call:incoming` | `{ callId, fromId, fromName, callType, roomName, sdpOffer, iceServers }` | Вхідний дзвінок |
| `call:answer` | `{ sdpAnswer, iceServers }` | Відповідь на дзвінок |
| `call:rejected` | `{ reason }` | Дзвінок відхилено |
| `ice:candidate` | `{ candidate, sdpMLineIndex, sdpMid }` | ICE candidate від іншого користувача |
| `call:ended` | `{ reason }` | Дзвінок завершено |

---

## 🧪 Тестування

### 1. Тестувати генерацію TURN credentials:

```bash
node generate-turn-credentials.js
```

**Output:**
```
🔐 TURN Credentials Generated:
Username: 1705488600:user123
Password: kF8vY2lMp...
Expires: 2024-01-18T10:30:00.000Z
```

### 2. Тестувати TURN сервер онлайн:

1. Відкрити: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
2. Додати TURN сервер:
   ```
   turn:worldmates.club:3478
   ```
3. Username/Password з попереднього кроку
4. Клацнути "Gather candidates"
5. Шукати `typ relay` - це означає TURN працює! ✅

### 3. Тестувати Socket.IO:

```javascript
const io = require('socket.io-client');
const socket = io('http://localhost:449');

socket.on('connect', () => {
    console.log('Connected!');
    socket.emit('user:join', 123);
});

socket.on('call:incoming', (data) => {
    console.log('Incoming call:', data);
});
```

---

## 📊 Моніторинг

### Логи TURN сервера:

```bash
sudo tail -f /var/log/turnserver.log
```

### Логи Node.js сервера:

```bash
tail -f server.log
```

### Статистика дзвінків:

```bash
curl http://localhost:449/api/admin/calls/stats | jq
```

### Статус сервісів:

```bash
# TURN сервер
sudo systemctl status coturn

# Node.js (якщо використовуєте PM2)
pm2 status
pm2 logs worldmates
```

---

## 🔐 Безпека

### 1. Змінити static-auth-secret:

```bash
# Згенерувати новий секрет
openssl rand -hex 32

# Оновити в turnserver.conf
sudo nano /etc/turnserver.conf

# Оновити в generate-turn-credentials.js
nano generate-turn-credentials.js

# Перезапустити
sudo systemctl restart coturn
pm2 restart worldmates
```

### 2. Обмежити доступ:

У `turnserver.conf`:
```ini
# Дозволити тільки з певних IP
allowed-peer-ip=YOUR_APP_SERVER_IP
```

### 3. Аутентифікація для /api/admin/*:

Додати middleware:
```javascript
app.use('/api/admin/*', (req, res, next) => {
    const token = req.headers.authorization;
    if (!token || !verifyAdminToken(token)) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
});
```

---

## 🚨 Troubleshooting

### TURN сервер не запускається:

```bash
# Перевірити порти
sudo netstat -tulpn | grep 3478

# Перевірити сертифікати
ls -la /var/www/httpd-cert/www-root/

# Перевірити логи
sudo journalctl -u coturn -n 50
```

### Node.js помилки:

```bash
# Перевірити порт 449
lsof -ti:449

# Перевірити логи
tail -f server.log

# Перезапустити
./start-all.sh
```

### Дзвінки не з'єднуються:

1. Перевірити що TURN працює: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
2. Перевірити що порти відкриті: `sudo ufw status`
3. Перевірити логи Android app: Logcat фільтр "WebRTC"
4. Перевірити Socket.IO з'єднання: `curl http://localhost:449/api/health`

---

## 📞 Production Deployment

### Using PM2:

```bash
# Встановити PM2
npm install -g pm2

# Запустити сервер
pm2 start server-example.js --name worldmates

# Auto-restart on reboot
pm2 startup
pm2 save

# Моніторинг
pm2 monit
```

### Using systemd:

Створити `/etc/systemd/system/worldmates-server.service`:

```ini
[Unit]
Description=WorldMates Node.js Server
After=network.target coturn.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/path/to/worldmates/server
ExecStart=/usr/bin/node server-example.js
Restart=always
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Запустити:
```bash
sudo systemctl daemon-reload
sudo systemctl enable worldmates-server
sudo systemctl start worldmates-server
```

---

## 📚 Ресурси

- [WebRTC Documentation](https://webrtc.org/)
- [coturn GitHub](https://github.com/coturn/coturn)
- [Socket.IO Documentation](https://socket.io/docs/v4/)
- [TURN Server Guide](https://www.html5rocks.com/en/tutorials/webrtc/infrastructure/)

---

## ✅ Чеклист запуску

- [ ] coturn встановлено
- [ ] `/etc/turnserver.conf` налаштовано з правильним `external-ip`
- [ ] `/etc/default/coturn` має `TURNSERVER_ENABLED=1`
- [ ] Порти відкриті (3478, 5349, 49152-65535, 449)
- [ ] SSL сертифікати шляхи правильні
- [ ] `npm install` виконано
- [ ] `./start-all.sh` запустився без помилок
- [ ] TURN тест пройшов: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
- [ ] Socket.IO працює: `curl http://localhost:449/api/health`

---

**Готово! Відеодзвінки тепер працюють! 🚀📞📹**
