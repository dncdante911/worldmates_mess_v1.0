# 🚀 WorldMates Video Calls - Швидкий старт

## ✅ ВСЕ ГОТОВО! Ось що було зроблено:

### 📱 Android App (повністю налаштовано):

1. ✅ **Settings** → Стиль рамок відеодзвінків
   - Користувач може вибрати: Classic, Neon, Gradient, Minimal, Glass, Rainbow

2. ✅ **Кнопки дзвінків активні**
   - 📞 Audio Call - запускає аудіодзвінок
   - 📹 Video Call - запускає відеодзвінок

3. ✅ **CallsActivity налаштовано**
   - Автоматична ініціація дзвінка
   - Request permissions (мікрофон + камера)
   - OutgoingCallScreen "Дзвонимо..."
   - ActiveCallScreen з кастомними рамками
   - PiP локальне відео (draggable)

4. ✅ **WebRTCManager оновлено**
   - Camera support (Camera2Enumerator)
   - switchCamera() метод
   - STUN сервери (Google - безкоштовно)
   - TURN ready (потрібно тільки налаштувати backend)

---

## 🔧 ЩО ПОТРІБНО ЗРОБИТИ (Backend):

### КРОК 1: Встановити TURN сервер на worldmates.club

```bash
# SSH до вашого сервера
ssh user@worldmates.club

# Встановити coturn
sudo apt-get update
sudo apt-get install coturn -y

# Скопіювати конфігурацію з репозиторію
cd /path/to/worldmates_mess_v1.0/server
sudo cp turnserver.conf /etc/turnserver.conf
```

### КРОК 2: Налаштувати External IP

```bash
# Дізнатися ваш публічний IP
curl ifconfig.me
# Припустимо отримали: 185.123.45.67

# Відредагувати конфіг
sudo nano /etc/turnserver.conf

# Знайти рядок:
# external-ip=YOUR_PUBLIC_IP_HERE

# Замінити на:
external-ip=185.123.45.67

# Зберегти: Ctrl+X, Y, Enter
```

### КРОК 3: Увімкнути coturn

```bash
# Відредагувати /etc/default/coturn
sudo nano /etc/default/coturn

# Знайти та розкоментувати (видалити #):
TURNSERVER_ENABLED=1

# Зберегти: Ctrl+X, Y, Enter
```

### КРОК 4: Відкрити порти у Firewall

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

### КРОК 5: Запустити TURN сервер

```bash
# Запустити
sudo systemctl start coturn

# Увімкнути автостарт
sudo systemctl enable coturn

# Перевірити статус
sudo systemctl status coturn

# Повинно бути: Active: active (running)
```

### КРОК 6: Запустити Node.js Backend

```bash
# Перейти до server директорії
cd /path/to/worldmates_mess_v1.0/server

# Встановити залежності
npm install

# Запустити ВСЕ одною командою
./start-all.sh

# Альтернативно вручну:
node server-example.js
```

**Готово! 🎉**

---

## 🧪 ТЕСТУВАННЯ

### Тест 1: Перевірити TURN сервер

1. Відкрити: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

2. Додати TURN сервер у поле "STUN or TURN URI":
   ```
   turn:worldmates.club:3478
   ```

3. Згенерувати Username та Password:
   ```bash
   cd /path/to/worldmates_mess_v1.0/server
   node generate-turn-credentials.js

   # Скопіювати Username та Password
   ```

4. Клацнути "Add Server"

5. Клацнути "Gather candidates"

6. Шукати у виводі:
   ```
   Done
   ...
   Time    Component    Type      Foundation    Protocol    Address    Port
   ...     ...          relay     ...           udp         ...        ...
   ```

   **Якщо бачите `typ relay` - TURN працює! ✅**

### Тест 2: Перевірити Node.js Server

```bash
# Health check
curl http://worldmates.club:449/api/health

# Повинно повернути:
# {"status":"ok","timestamp":"...","uptime":...}

# ICE servers
curl http://worldmates.club:449/api/ice-servers/123

# Повинно повернути TURN credentials
```

### Тест 3: Зробити тестовий відеодзвінок

1. Встановити WorldMates app на 2 пристрої
2. Увійти різними акаунтами
3. Відкрити чат між ними
4. Натиснути 📹 Video Call
5. Прийняти на другому пристрої
6. **Відео повинно з'єднатися!** 🎥

---

## 📋 static-auth-secret - ЩО ЦЕ?

**static-auth-secret** - це просто секретний ключ для генерації тимчасових TURN credentials.

### Як працює:

1. Android app запитує: `GET /api/ice-servers/userId`
2. Node.js генерує:
   - Username: `timestamp:userId` (наприклад: `1705488600:123`)
   - Password: `HMAC-SHA1(secret, username)` у base64
3. TURN сервер перевіряє password використовуючи той самий secret
4. Якщо співпадає - пропускає трафік

### Ваш згенерований secret:
```
a7f3e9c2d8b4f6a1c5e8d9b2f4a6c8e1d3f5a7b9c2e4f6a8b1d3f5a7c9e2f4a6
```

**⚠️ ТРИМАЙТЕ ЦЕЙ КЛЮЧ У СЕКРЕТІ!**

Він вже встановлений у:
- `turnserver.conf` (TURN сервер)
- `generate-turn-credentials.js` (Node.js)

### Якщо хочете змінити:

```bash
# Згенерувати новий
openssl rand -hex 32

# Скопіювати та замінити у обох файлах
sudo nano /etc/turnserver.conf  # Знайти static-auth-secret
nano generate-turn-credentials.js  # Знайти TURN_SECRET

# Перезапустити
sudo systemctl restart coturn
pm2 restart worldmates  # або перезапустити Node.js
```

---

## 🎯 ЧЕКЛИСТ ЗАПУСКУ

Перевірте всі пункти:

**TURN Сервер:**
- [ ] coturn встановлено: `dpkg -l | grep coturn`
- [ ] external-ip встановлено у `/etc/turnserver.conf`
- [ ] TURNSERVER_ENABLED=1 у `/etc/default/coturn`
- [ ] coturn запущено: `sudo systemctl status coturn`
- [ ] Порти відкриті: `sudo ufw status | grep 3478`

**Node.js Backend:**
- [ ] npm залежності встановлені: `ls node_modules`
- [ ] Сервер запущено: `curl http://localhost:449/api/health`
- [ ] Socket.IO працює (перевірити у браузері console)

**TURN Тестування:**
- [ ] webrtc.github.io тест показує `typ relay` ✅
- [ ] Credentials генеруються: `node generate-turn-credentials.js`

**Android App:**
- [ ] Settings → Стиль рамок відеодзвінків відкривається
- [ ] Кнопки Call/Video Call працюють
- [ ] Permissions запитуються при дзвінку

---

## 📞 СТРУКТУРА ФАЙЛІВ

```
worldmates_mess_v1.0/
├── app/                                   # Android App
│   └── src/main/java/.../
│       ├── ui/calls/
│       │   ├── CallsActivity.kt          # ✅ Налаштовано
│       │   └── CallsViewModel.kt         # ✅ Налаштовано
│       ├── ui/settings/
│       │   ├── SettingsActivity.kt       # ✅ Додано CallFrameStyle
│       │   └── CallFrameSettingsScreen.kt # ✅ НОВИЙ екран
│       └── network/
│           └── WebRTCManager.kt          # ✅ Camera support
│
└── server/                                # Backend
    ├── turnserver.conf                   # ✅ TURN конфігурація
    ├── generate-turn-credentials.js      # ✅ Dynamic credentials
    ├── callsSocketHandler.js             # ✅ Socket.IO handlers
    ├── server-example.js                 # ✅ Node.js server
    ├── start-all.sh                      # ✅ Автостарт скрипт
    ├── package.json                      # ✅ NPM config
    ├── README.md                         # 📚 Документація
    └── TURN_SETUP_INSTRUCTIONS.md        # 📚 Детальна інструкція
```

---

## 🚨 TROUBLESHOOTING

### Проблема: TURN тест не показує `typ relay`

**Рішення:**
```bash
# Перевірити логи
sudo tail -f /var/log/turnserver.log

# Перевірити що порти слухаються
sudo netstat -tulpn | grep turnserver

# Перезапустити
sudo systemctl restart coturn
```

### Проблема: Node.js сервер не запускається

**Рішення:**
```bash
# Перевірити що порт 449 вільний
lsof -ti:449

# Якщо зайнятий - вбити процес
kill -9 $(lsof -ti:449)

# Запустити знову
./start-all.sh
```

### Проблема: Дзвінки не з'єднуються

**Діагностика:**
1. Перевірити TURN: https://webrtc.github.io/samples/.../trickle-ice/
2. Перевірити Socket.IO: `curl http://worldmates.club:449/api/health`
3. Перевірити Android логи: Logcat фільтр "WebRTC" або "CallsActivity"
4. Перевірити що обидва пристрої онлайн у Socket.IO

---

## 📚 ДОДАТКОВА ДОКУМЕНТАЦІЯ

- **Детальна інструкція:** `server/TURN_SETUP_INSTRUCTIONS.md`
- **Backend README:** `server/README.md`
- **WebRTC Documentation:** https://webrtc.org/

---

## 🎉 ГОТОВО!

Після виконання всіх кроків:

1. ✅ Користувачі можуть робити відеодзвінки
2. ✅ Аудіодзвінки працюють
3. ✅ Дзвінки працюють навіть між різними мережами (завдяки TURN)
4. ✅ Кастомні рамки для відео (6 стилів на вибір)
5. ✅ PiP локальне відео (draggable)
6. ✅ Жести: double-tap fullscreen, swipe switch camera

**WorldMates тепер має унікальні відеодзвінки, яких немає у Telegram/Viber! 🚀📞📹**

---

**Commits:**
- `5779e93` - Відеодзвінки UI + Settings інтеграція
- `47ea5aa` - switchCamera() fix + CallFrameSettingsScreen
- `5fb65bf` - Відеодзвінки з кастомними рамками
- `76d24e7` - TURN server backend (цей commit)

**Branch:** `claude/messenger-feature-checklist-Vhc9Z`
