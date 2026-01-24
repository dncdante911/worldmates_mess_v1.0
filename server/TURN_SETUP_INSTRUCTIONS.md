# 🔧 TURN Server Setup Instructions for WorldMates

## 📋 Огляд

Цей документ містить покрокові інструкції для налаштування TURN сервера для відеодзвінків у WorldMates Messenger.

---

## 1️⃣ Встановлення TURN сервера (coturn)

### На Ubuntu/Debian:

```bash
sudo apt-get update
sudo apt-get install coturn -y
```

### Увімкнути coturn service:

```bash
# Відредагувати /etc/default/coturn
sudo nano /etc/default/coturn

# Знайти та розкоментувати:
TURNSERVER_ENABLED=1

# Зберегти (Ctrl+X, Y, Enter)
```

---

## 2️⃣ Налаштування TURN сервера

### Скопіювати конфігурацію:

```bash
# Backup оригінального конфігу
sudo cp /etc/turnserver.conf /etc/turnserver.conf.backup

# Скопіювати нашу конфігурацію
sudo cp /path/to/worldmates/server/turnserver.conf /etc/turnserver.conf
```

### ⚠️ ВАЖЛИВО: Знайти ваш публічний IP:

```bash
curl ifconfig.me
# Або
curl icanhazip.com
```

### Відредагувати `/etc/turnserver.conf`:

```bash
sudo nano /etc/turnserver.conf

# Знайти рядок:
external-ip=YOUR_PUBLIC_IP_HERE

# Замінити на ваш реальний IP, наприклад:
external-ip=123.45.67.89

# Зберегти (Ctrl+X, Y, Enter)
```

### Перевірити шлях до сертифікатів:

```bash
# Перевірити що файли існують:
ls -la /var/www/httpd-cert/www-root/worldmates.club_le2.crt
ls -la /var/www/httpd-cert/www-root/worldmates.club_le2.key

# Якщо файлів немає, знайти їх:
sudo find / -name "worldmates.club*.crt" 2>/dev/null
```

---

## 3️⃣ Відкрити порти у Firewall

### Для UFW:

```bash
# TURN порти
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp  # TLS

# Діапазон портів для relay
sudo ufw allow 49152:65535/udp
sudo ufw allow 49152:65535/tcp

# Reload firewall
sudo ufw reload
```

### Для iptables:

```bash
# TURN порти
sudo iptables -A INPUT -p tcp --dport 3478 -j ACCEPT
sudo iptables -A INPUT -p udp --dport 3478 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 5349 -j ACCEPT

# Діапазон портів для relay
sudo iptables -A INPUT -p udp --dport 49152:65535 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 49152:65535 -j ACCEPT

# Зберегти правила
sudo iptables-save > /etc/iptables/rules.v4
```

---

## 4️⃣ Запустити TURN сервер

```bash
# Запустити сервіс
sudo systemctl start coturn

# Увімкнути автостарт
sudo systemctl enable coturn

# Перевірити статус
sudo systemctl status coturn

# Переглянути логи
sudo tail -f /var/log/turnserver.log
```

---

## 5️⃣ Тестування TURN сервера

### Використати онлайн тестер:

1. Відкрити: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
2. Додати ваш TURN сервер:
   ```
   turn:worldmates.club:3478
   ```
3. Username та Password згенерувати через:
   ```bash
   cd /path/to/worldmates/server
   node generate-turn-credentials.js
   ```
4. Натиснути "Gather candidates"
5. Шукати `typ relay` - це означає що TURN працює!

### Ручне тестування:

```bash
# Встановити turnutils
sudo apt-get install turnutils-uclient -y

# Тестувати TURN сервер
turnutils-uclient -v -u "test" -w "test123" worldmates.club
```

---

## 6️⃣ Node.js Integration

### Встановити в основний server.js:

```javascript
// server.js або app.js

const express = require('express');
const http = require('http');
const socketIO = require('socket.io');

// Імпорт обробників дзвінків
const { initializeCallsHandler, getActiveCallsStats } = require('./callsSocketHandler');

const app = express();
const server = http.createServer(app);
const io = socketIO(server, {
    cors: {
        origin: "*", // В продакшні обмежити до вашого домену
        methods: ["GET", "POST"]
    }
});

// Ініціалізувати обробники відеодзвінків
initializeCallsHandler(io);

// API endpoint для отримання ICE configuration
const { getIceServers } = require('./generate-turn-credentials');

app.get('/api/ice-servers/:userId', (req, res) => {
    const userId = req.params.userId;
    const iceServers = getIceServers(userId);
    res.json({ iceServers });
});

// API endpoint для статистики дзвінків (для адміністраторів)
app.get('/api/admin/calls/stats', (req, res) => {
    const stats = getActiveCallsStats();
    res.json(stats);
});

// Запустити сервер
const PORT = process.env.PORT || 449;
server.listen(PORT, () => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`📞 WebRTC calls enabled with TURN server`);
});
```

### Запустити Node.js сервер:

```bash
cd /path/to/worldmates/server
npm install socket.io express
node server.js
```

---

## 7️⃣ Оновити Android App (WebRTCManager.kt)

Файл вже оновлено у коміті, але переконайтеся що:

```kotlin
// WebRTCManager.kt

private val iceServers = listOf(
    // Google STUN
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),

    // ВАШ TURN сервер
    PeerConnection.IceServer.builder("turn:worldmates.club:3478")
        .setUsername("GENERATED_USERNAME")  // Від сервера
        .setPassword("GENERATED_PASSWORD")  // Від сервера
        .createIceServer(),

    // TLS TURN (більш безпечний)
    PeerConnection.IceServer.builder("turns:worldmates.club:5349")
        .setUsername("GENERATED_USERNAME")
        .setPassword("GENERATED_PASSWORD")
        .createIceServer()
)
```

**💡 Credentials генеруються динамічно на сервері через `/api/ice-servers/:userId`**

---

## 8️⃣ Моніторинг та Debug

### Переглянути активні з'єднання:

```bash
# Логи TURN сервера
sudo tail -f /var/log/turnserver.log

# Перевірити порти
sudo netstat -tulpn | grep turnserver
```

### Debug у Android app:

```kotlin
// У CallsViewModel.kt додайте:
Log.d("WebRTC", "ICE Servers: $iceServers")
Log.d("WebRTC", "Connection State: $connectionState")
```

### Перевірити статистику дзвінків:

```bash
curl http://worldmates.club:449/api/admin/calls/stats
```

---

## 9️⃣ Безпека та Продакшн

### Обмежити доступ до TURN (optional):

У `turnserver.conf` розкоментуйте:

```ini
# Дозволити тільки з певних IP
allowed-peer-ip=YOUR_APP_SERVER_IP
```

### Ротація static-auth-secret:

```bash
# Згенерувати новий секрет
openssl rand -hex 32

# Оновити в turnserver.conf
sudo nano /etc/turnserver.conf
# Змінити static-auth-secret

# Оновити в generate-turn-credentials.js
nano generate-turn-credentials.js
# Змінити TURN_SECRET

# Перезапустити сервіси
sudo systemctl restart coturn
pm2 restart server  # або ваш Node.js process manager
```

### Автоматичне оновлення SSL сертифікатів:

```bash
# Додати post-renewal hook для certbot
sudo nano /etc/letsencrypt/renewal-hooks/post/restart-turn.sh

#!/bin/bash
systemctl restart coturn

# Зробити виконуваним
sudo chmod +x /etc/letsencrypt/renewal-hooks/post/restart-turn.sh
```

---

## 🎯 Чеклист запуску

- [ ] coturn встановлено
- [ ] `/etc/turnserver.conf` налаштовано з правильним IP
- [ ] Сертифікати шляхи правильні
- [ ] Порти відкриті у firewall (3478, 5349, 49152-65535)
- [ ] TURN сервер запущено (`sudo systemctl status coturn`)
- [ ] Node.js обробники додано (`callsSocketHandler.js`)
- [ ] WebRTCManager.kt оновлено з TURN credentials
- [ ] Протестовано через https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

---

## 📞 Підтримка

Якщо виникають проблеми:

1. Перевірити логи: `sudo tail -f /var/log/turnserver.log`
2. Тестувати TURN: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
3. Перевірити що порти відкриті: `sudo netstat -tulpn | grep 3478`
4. Debug Android app: Увімкнити verbose логування у CallsViewModel

---

**Готово! Відеодзвінки тепер працюватимуть навіть між різними мережами! 🚀**
