# 🚀 Швидкий старт: Налаштування відеодзвінків WorldMates

## ✅ Що було зроблено

### 1. Backend інтеграція (ГОТОВО ✅)

Ваш існуючий Node.js backend (`api-server-files/nodejs/`) тепер підтримує TURN сервер:

**Додані файли**:
- `helpers/turn-credentials.js` - генерація TURN credentials
- `TURN_INTEGRATION_GUIDE.md` - повна документація

**Оновлені файли**:
- `main.js` - додано REST API endpoints (`/api/ice-servers/:userId`)
- `listeners/calls-listener.js` - автоматична передача TURN credentials у Socket.IO events

### 2. Android додаток (ГОТОВО ✅)

- ✅ Кастомні рамки відеодзвінків (6 стилів)
- ✅ Settings для вибору стилю рамки
- ✅ Активна кнопка відеодзвінків у MessagesScreen
- ✅ Автоматична ініціація дзвінків
- ✅ Підтримка audio/video дзвінків
- ✅ Picture-in-Picture для локального відео
- ✅ Переключення камер (front/back)

---

## 🔧 Що потрібно налаштувати (2 кроки!)

### Крок 1: Налаштувати TURN сервер (coturn)

#### 1.1. Встановити coturn
```bash
sudo apt-get update
sudo apt-get install coturn -y
```

#### 1.2. Скопіювати конфігурацію
```bash
# Backup старого конфігу
sudo cp /etc/turnserver.conf /etc/turnserver.conf.backup

# Скопіювати новий конфіг
sudo cp /home/user/worldmates_mess_v1.0/server/turnserver.conf /etc/turnserver.conf
```

#### 1.3. Встановити ваш публічний IP
```bash
# Отримати IP
curl ifconfig.me

# Відредагувати конфіг
sudo nano /etc/turnserver.conf

# Знайти рядок:
# external-ip=YOUR_PUBLIC_IP_HERE
# Замінити на ваш реальний IP, наприклад:
# external-ip=123.45.67.89
```

#### 1.4. Перевірити SSL сертифікати

У `/etc/turnserver.conf` перевірте шляхи:
```bash
# Перевірити що існують:
ls -la /var/www/httpd-cert/www-root/worldmates.club_le2.crt
ls -la /var/www/httpd-cert/www-root/worldmates.club_le2.key

# Якщо файлів немає, знайти їх:
sudo find / -name "worldmates.club*.crt" 2>/dev/null

# Оновити шляхи в конфігу якщо потрібно
```

#### 1.5. Увімкнути coturn
```bash
# Відредагувати /etc/default/coturn
sudo nano /etc/default/coturn

# Розкоментувати:
TURNSERVER_ENABLED=1

# Зберегти (Ctrl+X, Y, Enter)
```

#### 1.6. Відкрити порти
```bash
# TURN порти
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp

# Relay порти
sudo ufw allow 49152:65535/udp
sudo ufw allow 49152:65535/tcp

sudo ufw reload
```

#### 1.7. Запустити TURN сервер
```bash
sudo systemctl start coturn
sudo systemctl enable coturn

# Перевірити статус
sudo systemctl status coturn

# Переглянути логи
sudo tail -f /var/log/turnserver.log
```

---

### Крок 2: Перезапустити Node.js сервер

```bash
# Якщо використовуєте PM2
pm2 restart worldmates

# Або вручну
cd /home/user/worldmates_mess_v1.0/api-server-files/nodejs
node main.js

# Перевірити що працює
curl http://localhost:449/api/health
curl http://localhost:449/api/ice-servers/123
```

---

## 🧪 Тестування

### 1. Тестувати TURN сервер онлайн

1. Відкрити: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

2. Згенерувати credentials:
```bash
cd /home/user/worldmates_mess_v1.0/api-server-files/nodejs
node -e "const t = require('./helpers/turn-credentials'); console.log(JSON.stringify(t.generateTurnCredentials(123), null, 2))"
```

3. Додати TURN сервер у форму:
   - URLs: `turn:worldmates.club:3478`
   - Username: з виводу команди вище
   - Password: з виводу команди вище

4. Клацнути "Gather candidates"

5. Шукати `typ relay` - якщо є, TURN працює! ✅

### 2. Тестувати REST API

```bash
# Health check
curl http://worldmates.club:449/api/health

# ICE servers для користувача 123
curl http://worldmates.club:449/api/ice-servers/123

# TURN credentials (POST)
curl -X POST http://worldmates.club:449/api/turn-credentials \
  -H "Content-Type: application/json" \
  -d '{"userId": "123"}'
```

### 3. Тестувати з Android додатком

1. Зібрати та встановити Android додаток
2. Увійти двома користувачами на різних пристроях
3. Один ініціює відеодзвінок
4. Другий отримує вхідний дзвінок
5. Прийняти дзвінок
6. Перевірити що відео працює
7. Перевірити переключення камер
8. Перевірити різні стилі рамок у Settings

---

## 📁 Структура файлів проекту

### Backend (ваш існуючий Node.js)
```
api-server-files/nodejs/
├── main.js                      # ✅ ОНОВЛЕНО - додано REST endpoints
├── listeners/
│   ├── listeners.js             # Реєстрація всіх listeners
│   └── calls-listener.js        # ✅ ОНОВЛЕНО - додано TURN credentials
├── helpers/
│   └── turn-credentials.js      # ✅ НОВИЙ - генерація TURN credentials
├── models/
│   ├── wo_calls.js              # ✅ Вже існує
│   ├── wo_group_calls.js        # ✅ Вже існує
│   └── wo_ice_candidates.js     # ✅ Вже існує
└── TURN_INTEGRATION_GUIDE.md    # ✅ НОВИЙ - повна документація
```

### TURN Server конфігурація
```
server/
├── turnserver.conf              # ⚠️ КОПІЮВАТИ в /etc/turnserver.conf
├── README.md                    # Документація (для довідки)
└── TURN_SETUP_INSTRUCTIONS.md   # Детальна інструкція (для довідки)
```

**ВАЖЛИВО**: Файли `server/callsSocketHandler.js`, `server-example.js`, `generate-turn-credentials.js` НЕ ПОТРІБНІ, бо у вас вже є повноцінний backend в `api-server-files/nodejs/`!

### Android додаток
```
app/src/main/java/com/worldmates/messenger/
├── network/
│   └── WebRTCManager.kt         # ✅ ОНОВЛЕНО - switchCamera()
├── ui/calls/
│   ├── CallsActivity.kt         # ✅ ОНОВЛЕНО - auto-initiate
│   ├── CallsViewModel.kt        # WebRTC логіка
│   ├── CallsScreen.kt           # ✅ ОНОВЛЕНО - різні стани
│   ├── ActiveCallScreen.kt      # Екран активного дзвінка
│   ├── RemoteVideoView.kt       # ✅ НОВИЙ - 6 кастомних рамок
│   └── LocalVideoPiP.kt         # ✅ НОВИЙ - Picture-in-Picture
├── ui/settings/
│   ├── SettingsActivity.kt      # ✅ ОНОВЛЕНО - меню стилів рамок
│   └── CallFrameSettingsScreen.kt  # ✅ НОВИЙ - вибір стилю рамки
└── ui/messages/
    ├── MessagesScreen.kt        # ✅ ОНОВЛЕНО - активна кнопка відео
    └── MessagesViewModel.kt     # ✅ ОНОВЛЕНО - getRecipientId()
```

---

## 🔐 Безпека

### ⚠️ ВАЖЛИВО: Змінити TURN secret

Файл `helpers/turn-credentials.js` та `/etc/turnserver.conf` мають однаковий секрет:
```
a7f3e9c2d8b4f6a1c5e8d9b2f4a6c8e1d3f5a7b9c2e4f6a8b1d3f5a7c9e2f4a6
```

**Для продакшну ОБОВ'ЯЗКОВО змініть на унікальний**:

```bash
# Згенерувати новий секрет
openssl rand -hex 32

# Оновити в обох місцях:
# 1. /etc/turnserver.conf → static-auth-secret=НОВИЙ_СЕКРЕТ
# 2. helpers/turn-credentials.js → const TURN_SECRET = 'НОВИЙ_СЕКРЕТ'

# Перезапустити сервіси
sudo systemctl restart coturn
pm2 restart worldmates  # або ваш метод
```

---

## 📊 Моніторинг

### Логи TURN сервера
```bash
sudo tail -f /var/log/turnserver.log
```

### Логи Node.js
```bash
pm2 logs worldmates  # якщо PM2
# або
tail -f /path/to/server.log
```

### Статус сервісів
```bash
# TURN сервер
sudo systemctl status coturn

# Node.js (якщо PM2)
pm2 status
```

---

## 🚨 Troubleshooting

### TURN сервер не запускається
```bash
# Перевірити логи
sudo journalctl -u coturn -n 50

# Перевірити конфігурацію
sudo turnserver -c /etc/turnserver.conf --log-file=stdout

# Перевірити порти
sudo netstat -tulpn | grep 3478
```

### Node.js помилки
```bash
# Перевірити порт 449
lsof -ti:449

# Перевірити логи
pm2 logs worldmates

# Перезапустити
pm2 restart worldmates
```

### Android додаток не з'єднується
1. Перевірити Socket.IO з'єднання: Logcat фільтр "WebRTC"
2. Перевірити що TURN працює: онлайн тестер (вище)
3. Перевірити що `/api/ice-servers/:userId` працює
4. Перевірити що Android отримує `iceServers` у `call:incoming` event

---

## 📚 Документація

- **TURN_INTEGRATION_GUIDE.md** - повна документація інтеграції з прикладами коду
- **server/README.md** - загальна інформація про TURN сервер
- **server/TURN_SETUP_INSTRUCTIONS.md** - детальні інструкції налаштування

---

## ✅ Чеклист

- [ ] coturn встановлено
- [ ] `/etc/turnserver.conf` налаштовано з правильним `external-ip`
- [ ] `/etc/default/coturn` має `TURNSERVER_ENABLED=1`
- [ ] Порти відкриті (3478, 5349, 49152-65535, 449)
- [ ] SSL сертифікати існують та шляхи правильні
- [ ] Node.js backend перезапущено
- [ ] TURN онлайн тест пройшов (`typ relay` знайдено)
- [ ] `/api/ice-servers/123` працює
- [ ] `/api/health` працює
- [ ] Android додаток зібрано та встановлено
- [ ] Відеодзвінки працюють між двома пристроями
- [ ] TURN secret змінено на унікальний (для продакшну)

---

**Готово! Після виконання 2 кроків вище (налаштування TURN + перезапуск Node.js), відеодзвінки будуть ПОВНІСТЮ робочими! 🚀📞📹**

Якщо виникнуть питання або проблеми - дивіться `api-server-files/nodejs/TURN_INTEGRATION_GUIDE.md` для детальної документації.
