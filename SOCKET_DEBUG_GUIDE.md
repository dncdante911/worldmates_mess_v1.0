# 🔍 Гайд по діагностиці Socket.IO

## Проблема: Немає логів від SocketManager

Якщо в `logcat` немає жодних логів від SocketManager/MessagesViewModel, це означає:

### Можливі причини:

1. ❌ **Додаток не запущений** або не в активному стані
2. ❌ **SocketManager не ініціалізується** (не відкрито чат)
3. ❌ **Проблема з SSL сертифікатом** (Node.js не може прийняти з'єднання)
4. ❌ **HAproxy блокує WebSocket** (неправильна конфігурація)

## 🛠️ Кроки діагностики:

### Крок 1: Запустіть скрипт діагностики

```bash
cd /home/user/worldmates_mess_v1.0
./debug_socket_connection.sh
```

Цей скрипт перевірить:
- ✅ Чи запущений додаток
- ✅ Чи є логи від додатка
- ✅ Чи відповідає сервер на порту 449
- ✅ Покаже логи в реальному часі

### Крок 2: Відкрийте чат в додатку

**ВАЖЛИВО:** SocketManager ініціалізується ТІЛЬКИ коли ви відкриваєте чат!

1. Відкрийте додаток WorldMates
2. Натисніть на будь-який чат
3. Почекайте 2-3 секунди
4. Перевірте логи:

```bash
adb logcat | grep "Socket"
```

Має з'явитись:
```
SocketManager: Socket Connected! ID: XXX
SocketManager: Sent 'join' event with session hash: ...
```

### Крок 3: Перевірка з'єднання з сервером

#### 3.1 Перевірка чи Node.js запущений на сервері:

```bash
# На сервері
ps aux | grep node
netstat -tlnp | grep 449
```

Має показати Node.js процес на порту 449.

#### 3.2 Перевірка SSL:

```bash
# З вашого комп'ютера
curl -k -v https://worldmates.club:449/

# Або через openssl
openssl s_client -connect worldmates.club:449
```

Має показати SSL handshake.

### Крок 4: Перевірка HAproxy

**ПРОБЛЕМА:** Ваш HAproxy в TCP mode, тому він НЕ робить SSL термінацію!

Це означає що **Node.js має сам обробляти SSL**.

Перевірте `main.js` на сервері:

```javascript
// Має бути щось таке:
const https = require('https');
const fs = require('fs');

const options = {
    key: fs.readFileSync('/path/to/private.key'),
    cert: fs.readFileSync('/path/to/certificate.crt')
};

const server = https.createServer(options, app);
const io = require('socket.io')(server);

server.listen(449);
```

Якщо Node.js НЕ налаштований для SSL, тоді:

**ВАРІАНТ А:** Налаштувати Node.js для SSL

**ВАРІАНТ Б:** Змінити HAproxy на HTTP mode з SSL термінацією:

```haproxy
frontend nodejs_449
    bind 195.22.131.11:449 ssl crt /etc/ssl/worldmates.pem
    bind 46.232.232.38:449 ssl crt /etc/ssl/worldmates.pem
    mode http  # Змінити на HTTP
    option http-server-close
    option forwardfor

    # WebSocket підтримка
    acl is_websocket hdr(Upgrade) -i WebSocket
    acl is_websocket hdr_beg(Host) -i ws

    default_backend nodejs_449_backend

backend nodejs_449_backend
    mode http  # Змінити на HTTP
    balance source

    timeout connect 10s
    timeout server 3h
    timeout client 3h
    timeout tunnel 3h

    # Node.js без SSL (HAproxy вже розшифрував)
    server nodejs_app 192.168.0.250:449 check inter 5s
```

### Крок 5: Тест без SSL (для діагностики)

Тимчасово змініть URL в Constants.kt:

```kotlin
// ТІЛЬКИ ДЛЯ ТЕСТУ!
const val SOCKET_URL = "http://worldmates.club:3000/"  // Без SSL
```

Якщо після цього з'єднання працює - проблема точно в SSL.

## 🔧 Швидкі виправлення:

### Виправлення 1: Примусова ініціалізація Socket при вході

Додайте в `MessagesViewModel.kt`:

```kotlin
init {
    Log.d("MessagesViewModel", "🚀 MessagesViewModel створено!")
}
```

Це допоможе побачити чи взагалі створюється ViewModel.

### Виправлення 2: Додайте простий тест в MessagesActivity

Додайте в `MessagesActivity.onCreate()`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Log.d("MessagesActivity", "🚀 Activity створено!")
    Log.d("MessagesActivity", "Socket URL: ${Constants.SOCKET_URL}")
    Log.d("MessagesActivity", "Access Token: ${UserSession.accessToken?.take(10)}...")

    // ... решта коду
}
```

### Виправлення 3: Перевірка чи є access token

```kotlin
// В SocketManager.connect()
if (UserSession.accessToken == null) {
    Log.e("SocketManager", "❌ Access token is NULL! Cannot connect.")
    return
}
Log.d("SocketManager", "✅ Access token OK: ${UserSession.accessToken.take(10)}...")
```

## 📊 Що має бути в логах (в правильному порядку):

```
1. MessagesActivity: 🚀 Activity створено!
2. MessagesViewModel: 🚀 MessagesViewModel створено!
3. SocketManager: ✅ Access token OK: d00d1617c8...
4. SocketManager: Socket Connected! ID: abc123
5. SocketManager: Sent 'join' event with session hash: d00d1617c8...
6. SocketManager: Received user_status_change event with 1 args
7. SocketManager: Parsed user 8 as ONLINE ✅
```

## ⚠️ Якщо все ще немає логів:

### Останній варіант: Увімкніть verbose логування

```bash
# Встановіть рівень логування на VERBOSE
adb shell setprop log.tag.SocketManager VERBOSE
adb shell setprop log.tag.MessagesViewModel VERBOSE

# Перезапустіть додаток
adb shell am force-stop com.worldmates.messenger
adb shell am start -n com.worldmates.messenger/.ui.MainActivity

# Дивіться логи
adb logcat -v time "*:V"
```

## 🆘 Контрольний список:

- [ ] Додаток запущений
- [ ] Відкрито чат (не просто список чатів!)
- [ ] Access token є (не null)
- [ ] Node.js запущений на порту 449
- [ ] SSL сертифікат валідний
- [ ] HAproxy проксує на правильний порт
- [ ] Фаєрвол не блокує порт 449
- [ ] WebSocket не блокується (немає CORS помилок)

---

**Створено:** 2025-12-26
**Автор:** Claude Code Agent
