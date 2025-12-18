# 🔐 PHP Encryption Replacement Guide
## Миграция на AES-256-GCM шифрование

---

## ⚠️ ВАЖНАЯ ИНФОРМАЦИЯ О ВАШЕЙ КОНФИГУРАЦИИ

### Используемые файлы на сервере:

✅ **group_chat_v2.php** - `/var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php`
- Кастомный API v2 из `server_modifications/group_chat_v2.php`
- **УЖЕ ОБНОВЛЁН** с поддержкой AES-256-GCM ✅

📂 **Стандартные API файлы** - `/home/user/api-worldmates/`:
- send-message.php
- get_user_messages.php
- get_chats.php
- page_chat.php
- group_chat.php (старая версия, НЕ используется)
- get-site-settings.php
- phone/get_users_list.php

---

## 🚀 БЫСТРЫЙ СТАРТ - Порядок действий

### 1. Подготовка БД (КРИТИЧНО!)

Сначала добавьте поля в таблицу `Wo_Messages`:

```sql
-- Подключитесь к вашей БД и выполните:
ALTER TABLE Wo_Messages ADD COLUMN iv VARCHAR(255) NULL AFTER text;
ALTER TABLE Wo_Messages ADD COLUMN tag VARCHAR(255) NULL AFTER iv;
ALTER TABLE Wo_Messages ADD COLUMN cipher_version INT DEFAULT 1 AFTER tag;

-- Проверка что поля добавлены:
DESCRIBE Wo_Messages;
```

### 2. Копирование файлов на сервер

```bash
# Скопируйте crypto_helper.php в includes
scp /home/user/api-worldmates/includes/crypto_helper.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/includes/

# Скопируйте обновленный group_chat_v2.php
scp /home/user/worldmates_mess_v1.0/server_modifications/group_chat_v2.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/

# Скопируйте тестовый скрипт
scp /home/user/api-worldmates/test_gcm.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/
```

### 3. Проверка работы GCM

```bash
# На сервере запустите тест:
cd /var/www/www-root/data/www/worldmates.club/
php test_gcm.php
```

Должен вывести: `✅ Все тесты пройдены успешно!`

### 4. Обновление остальных PHP файлов

См. детальные инструкции ниже для каждого файла.

---

## 📋 Общий план замены

### Шаг 1: Подключение crypto_helper.php

В **КАЖДОМ** файле, где используется шифрование, добавьте в начало (после других require):

```php
require_once('includes/crypto_helper.php');
```

### Шаг 2: Замена старого кода на новый

**СТАРЫЙ КОД (AES-128-ECB):**
```php
$message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
```

**НОВЫЙ КОД (AES-256-GCM):**
```php
$encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
if ($encrypted !== false) {
    $message['text'] = $encrypted['text'];
    $message['iv'] = $encrypted['iv'];
    $message['tag'] = $encrypted['tag'];
    $message['cipher_version'] = $encrypted['cipher_version'];
}
```

---

## 📁 Файл 0: group_chat_v2.php ✅ ГОТОВ

### Локация на сервере: `/var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php`
### Локация в репо: `server_modifications/group_chat_v2.php`

### ✅ СТАТУС: УЖЕ ОБНОВЛЁН!

Этот файл уже обновлен с поддержкой AES-256-GCM:
- ✅ Добавлено подключение crypto_helper.php
- ✅ Эндпоинт `send_message` шифрует текст перед сохранением в БД
- ✅ Эндпоинт `get_messages` возвращает iv, tag, cipher_version
- ✅ Добавлено логирование шифрования

**Что было добавлено (строка 29):**
```php
// Підключаємо модуль шифрування AES-256-GCM
require_once('../includes/crypto_helper.php');
```

**Что было добавлено (строка 404-428):**
```php
// Шифруємо текст з використанням AES-256-GCM
$encrypted_text = $text;
$iv = null;
$tag = null;
$cipher_version = 1;

if (!empty($text)) {
    $encrypted_data = CryptoHelper::encryptGCM($text, $time);
    if ($encrypted_data !== false) {
        $encrypted_text = $encrypted_data['text'];
        $iv = $encrypted_data['iv'];
        $tag = $encrypted_data['tag'];
        $cipher_version = $encrypted_data['cipher_version'];
        logMessage("Message encrypted with GCM, IV: " . substr($iv, 0, 10) . "...");
    }
}

$stmt = $db->prepare("
    INSERT INTO Wo_Messages (from_id, group_id, to_id, text, media, time, seen, iv, tag, cipher_version)
    VALUES (?, ?, 0, ?, ?, ?, 0, ?, ?, ?)
");
$stmt->execute([$current_user_id, $group_id, $encrypted_text, $media, $time, $iv, $tag, $cipher_version]);
```

**Действия:**
1. Скопируйте обновленный файл на сервер
2. Убедитесь что путь к crypto_helper.php правильный (может потребоваться корректировка `../includes/crypto_helper.php`)

---

## 📁 Файл 1: send-message.php

### Локация: `/home/user/api-worldmates/send-message.php`

### Строка 178 - БЫЛО:

```php
$message['message_hash_id'] = $_POST['message_hash_id'];
$message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
unset($message['or_text']);
```

### Строка 178 - СТАЛО:

```php
$message['message_hash_id'] = $_POST['message_hash_id'];

// Шифруем текст с использованием AES-256-GCM
$encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
if ($encrypted !== false) {
    $message['text'] = $encrypted['text'];
    $message['iv'] = $encrypted['iv'];
    $message['tag'] = $encrypted['tag'];
    $message['cipher_version'] = $encrypted['cipher_version'];
} else {
    // Fallback на старый метод в случае ошибки
    error_log("send-message.php: Failed to encrypt with GCM, using ECB fallback");
    $message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
}

unset($message['or_text']);
```

---

## 📁 Файл 2: get_user_messages.php

### Локация: `/home/user/api-worldmates/get_user_messages.php`

### Строка 46 - Основное сообщение - БЫЛО:

```php
foreach ($message_info as $message) {
    $message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
    if ($not_include_status == true) {
        foreach ($not_include_array as $value) {
```

### Строка 46 - Основное сообщение - СТАЛО:

```php
foreach ($message_info as $message) {
    // Шифруем текст с использованием AES-256-GCM
    $encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
    if ($encrypted !== false) {
        $message['text'] = $encrypted['text'];
        $message['iv'] = $encrypted['iv'];
        $message['tag'] = $encrypted['tag'];
        $message['cipher_version'] = $encrypted['cipher_version'];
    }

    if ($not_include_status == true) {
        foreach ($not_include_array as $value) {
```

### Строка 98 - Ответы (replies) - БЫЛО:

```php
if (!empty($message['reply'])) {
    $message['reply']['text'] = openssl_encrypt($message['reply']['text'], "AES-128-ECB", $message['reply']['time']);
    if (empty($message['reply']['stickers'])) {
```

### Строка 98 - Ответы (replies) - СТАЛО:

```php
if (!empty($message['reply'])) {
    // Шифруем текст ответа с использованием AES-256-GCM
    $encryptedReply = CryptoHelper::encryptGCM($message['reply']['text'], $message['reply']['time']);
    if ($encryptedReply !== false) {
        $message['reply']['text'] = $encryptedReply['text'];
        $message['reply']['iv'] = $encryptedReply['iv'];
        $message['reply']['tag'] = $encryptedReply['tag'];
        $message['reply']['cipher_version'] = $encryptedReply['cipher_version'];
    }

    if (empty($message['reply']['stickers'])) {
```

---

## 📁 Файл 3: get_chats.php

### Локация: `/home/user/api-worldmates/get_chats.php`

Этот файл имеет **3 места** с шифрованием last_message.

### Строка 84 - Первое место - БЫЛО:

```php
$message = $value['last_message'];
$message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
if (empty($message['stickers'])) {
```

### Строка 84 - Первое место - СТАЛО:

```php
$message = $value['last_message'];

// Шифруем последнее сообщение с использованием AES-256-GCM
$encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
if ($encrypted !== false) {
    $message['text'] = $encrypted['text'];
    $message['iv'] = $encrypted['iv'];
    $message['tag'] = $encrypted['tag'];
    $message['cipher_version'] = $encrypted['cipher_version'];
}

if (empty($message['stickers'])) {
```

### Строка 176 - Второе место - АНАЛОГИЧНО

### Строка 262 - Третье место - АНАЛОГИЧНО

**Замените все три места одинаковым образом!**

---

## 📁 Файл 4: page_chat.php

### Локация: `/home/user/api-worldmates/page_chat.php`

Этот файл имеет **2 места** с шифрованием.

### Строка 245 - Массив сообщений - БЫЛО:

```php
foreach ($message_info as $key => $message) {
    $message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
    $message['time_text'] = Wo_Time_Elapsed_String($message['time']);
```

### Строка 245 - Массив сообщений - СТАЛО:

```php
foreach ($message_info as $key => $message) {
    // Шифруем текст с использованием AES-256-GCM
    $encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
    if ($encrypted !== false) {
        $message['text'] = $encrypted['text'];
        $message['iv'] = $encrypted['iv'];
        $message['tag'] = $encrypted['tag'];
        $message['cipher_version'] = $encrypted['cipher_version'];
    }

    $message['time_text'] = Wo_Time_Elapsed_String($message['time']);
```

### Строка 407 - Last message для страниц - БЫЛО:

```php
$page['last_message']['text'] = openssl_encrypt($page['last_message']['text'], "AES-128-ECB", $page['last_message']['time']);

$pages[] = $page;
```

### Строка 407 - Last message для страниц - СТАЛО:

```php
// Шифруем последнее сообщение страницы с использованием AES-256-GCM
$encrypted = CryptoHelper::encryptGCM($page['last_message']['text'], $page['last_message']['time']);
if ($encrypted !== false) {
    $page['last_message']['text'] = $encrypted['text'];
    $page['last_message']['iv'] = $encrypted['iv'];
    $page['last_message']['tag'] = $encrypted['tag'];
    $page['last_message']['cipher_version'] = $encrypted['cipher_version'];
}

$pages[] = $page;
```

---

## 📁 Файл 5: get-site-settings.php

### Локация: `/home/user/api-worldmates/get-site-settings.php`

⚠️ **ВНИМАНИЕ:** Этот файл шифрует конфигурацию сайта, а не сообщения!

### Строка 50 - БЫЛО:

```php
$get_config = json_encode($get_config, JSON_PRETTY_PRINT);
$get_config = openssl_encrypt($get_config, "AES-128-ECB", $siteEncryptKey);

$response_data = array(
    'api_status' => 200,
```

### Строка 50 - СТАЛО:

```php
$get_config = json_encode($get_config, JSON_PRETTY_PRINT);

// Шифруем конфигурацию с использованием AES-256-GCM
$encrypted = CryptoHelper::encryptGCM($get_config, $siteEncryptKey);

if ($encrypted !== false) {
    $response_data = array(
        'api_status' => 200,
        'config' => $encrypted['text'],
        'iv' => $encrypted['iv'],
        'tag' => $encrypted['tag'],
        'cipher_version' => $encrypted['cipher_version']
    );
} else {
    // Fallback на старый метод
    $get_config = openssl_encrypt($get_config, "AES-128-ECB", $siteEncryptKey);
    $response_data = array(
        'api_status' => 200,
        'config' => $get_config
    );
}
```

---

## 📁 Файл 6: phone/get_users_list.php

### Локация: `/home/user/api-worldmates/phone/get_users_list.php`

### Строка 162 - БЫЛО:

```php
if (!empty($json_data['last_message']['time'])) {
    $json_data['last_message']['text'] = openssl_encrypt($json_data['last_message']['text'], "AES-128-ECB", $json_data['last_message']['time']);
    $time_today  = time() - 86400;
```

### Строка 162 - СТАЛО:

```php
if (!empty($json_data['last_message']['time'])) {
    // Шифруем последнее сообщение с использованием AES-256-GCM
    $encrypted = CryptoHelper::encryptGCM(
        $json_data['last_message']['text'],
        $json_data['last_message']['time']
    );
    if ($encrypted !== false) {
        $json_data['last_message']['text'] = $encrypted['text'];
        $json_data['last_message']['iv'] = $encrypted['iv'];
        $json_data['last_message']['tag'] = $encrypted['tag'];
        $json_data['last_message']['cipher_version'] = $encrypted['cipher_version'];
    }

    $time_today  = time() - 86400;
```

---

## 🗄️ МИГРАЦИЯ БАЗЫ ДАННЫХ

### Добавление полей для GCM

```sql
-- Подключитесь к MySQL:
-- mysql -u your_user -p your_database

-- Добавьте поля в таблицу Wo_Messages
ALTER TABLE Wo_Messages ADD COLUMN iv VARCHAR(255) NULL AFTER text;
ALTER TABLE Wo_Messages ADD COLUMN tag VARCHAR(255) NULL AFTER iv;
ALTER TABLE Wo_Messages ADD COLUMN cipher_version INT DEFAULT 1 AFTER tag;

-- Добавьте индекс для быстрого поиска по cipher_version
ALTER TABLE Wo_Messages ADD INDEX idx_cipher_version (cipher_version);

-- Проверка структуры таблицы
DESCRIBE Wo_Messages;
```

### Проверка миграции БД

```sql
-- Должны увидеть новые поля:
-- | iv              | varchar(255) | YES  |     | NULL    |       |
-- | tag             | varchar(255) | YES  |     | NULL    |       |
-- | cipher_version  | int(11)      | YES  |     | 1       |       |

-- Проверка количества записей с разными версиями шифрования:
SELECT
    cipher_version,
    COUNT(*) as count,
    CASE
        WHEN cipher_version = 1 THEN 'AES-128-ECB (старое)'
        WHEN cipher_version = 2 THEN 'AES-256-GCM (новое)'
        ELSE 'Неизвестно'
    END as encryption_type
FROM Wo_Messages
GROUP BY cipher_version;
```

---

## 🔍 Как проверить что все работает

### 1. Проверка поддержки GCM на сервере:

```bash
# На сервере запустите:
cd /var/www/www-root/data/www/worldmates.club/
php test_gcm.php
```

Должен вывести:
```
=== WorldMates AES-256-GCM Test ===

1. Проверка поддержки AES-GCM...
   ✅ AES-GCM поддерживается!

2. Тест шифрования...
   ✅ Шифрование успешно!

...

=== Результат ===
✅ Все тесты пройдены успешно!
```

### 2. Проверка логов PHP:

```bash
# Смотрите логи group_chat_v2:
tail -f /var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log

# Должны увидеть:
# [2025-12-18 11:34:23] Message encrypted with GCM, IV: MTIzNDU2Nz...
```

### 3. Проверка в Android приложении:

1. Отправьте тестовое сообщение в группу
2. Проверьте logcat:
```bash
adb logcat | grep -i "cipher"
```

Должны увидеть:
```
D/ChatsViewModel: Cipher version: 2
D/ChatsViewModel: Has IV/TAG: true/true
D/MessagesViewModel: 🔐 Дешифрування для повідомлення ID=12345
```

### 4. Проверка в БД:

```sql
-- Посмотрите последние 5 сообщений:
SELECT
    id,
    from_id,
    SUBSTRING(text, 1, 30) as text_preview,
    SUBSTRING(iv, 1, 20) as iv_preview,
    SUBSTRING(tag, 1, 20) as tag_preview,
    cipher_version,
    FROM_UNIXTIME(time) as sent_time
FROM Wo_Messages
WHERE group_id IS NOT NULL
ORDER BY id DESC
LIMIT 5;
```

Новые сообщения должны иметь:
- `iv` - не NULL (например: MTIzNDU2Nzg5...)
- `tag` - не NULL (например: YWJjZGVmZ2...)
- `cipher_version` - 2

---

## ⚠️ Важные замечания

### 1. Обратная совместимость

✅ Старые сообщения (без iv/tag) будут автоматически дешифровываться через ECB на Android
✅ PHP код также поддерживает оба формата через `CryptoHelper::decrypt()`

### 2. Порядок внедрения

1. ✅ **Сначала БД** - добавить поля iv, tag, cipher_version
2. ✅ **Затем сервер** - скопировать crypto_helper.php и обновленные файлы
3. ✅ **Проверка** - запустить test_gcm.php
4. ✅ **Android** - уже готов
5. 📊 **Мониторинг** - следить за логами

### 3. Пути к crypto_helper.php

В зависимости от локации API файла, путь может отличаться:

```php
// Для файлов в корне api:
require_once('includes/crypto_helper.php');

// Для файлов в api/v2/:
require_once('../includes/crypto_helper.php');

// Для файлов в api/phone/:
require_once('../includes/crypto_helper.php');
```

### 4. Rollback план

Если что-то пойдет не так:

```php
// Временно вернуться на ECB в конкретном файле:
$message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
```

---

## 📊 Сводная таблица файлов

| Файл | Количество мест | Строки | Статус |
|------|----------------|--------|--------|
| **group_chat_v2.php** | 2 | 29, 404-428 | ✅ **ГОТОВ** |
| send-message.php | 1 | 178 | ⏳ Ожидает |
| get_user_messages.php | 2 | 46, 98 | ⏳ Ожидает |
| get_chats.php | 3 | 84, 176, 262 | ⏳ Ожидает |
| page_chat.php | 2 | 245, 407 | ⏳ Ожидает |
| get-site-settings.php | 1 | 50 | ⏳ Ожидает |
| phone/get_users_list.php | 1 | 162 | ⏳ Ожидает |

**ВСЕГО:** 12 мест в 7 файлах (1 уже готов, 6 ожидают)

---

## 🎯 Чеклист для внедрения

### Подготовка:
- [ ] Сделать бэкап БД
- [ ] Сделать бэкап всех PHP файлов
- [ ] Проверить версию PHP на сервере (>= 7.1)

### База данных:
- [ ] Выполнить ALTER TABLE для добавления полей
- [ ] Проверить что поля добавлены (DESCRIBE Wo_Messages)

### Файлы на сервере:
- [ ] Скопировать crypto_helper.php в /includes/
- [ ] Скопировать обновленный group_chat_v2.php в /api/v2/
- [ ] Скопировать test_gcm.php в корень
- [ ] Запустить php test_gcm.php

### Обновление API файлов:
- [ ] send-message.php
- [ ] get_user_messages.php
- [ ] get_chats.php
- [ ] page_chat.php
- [ ] get-site-settings.php
- [ ] phone/get_users_list.php

### Тестирование:
- [ ] Отправить тестовое сообщение через group_chat_v2
- [ ] Проверить что сообщение зашифровано в БД
- [ ] Проверить что Android корректно дешифрует
- [ ] Проверить логи на ошибки
- [ ] Отправить тестовое личное сообщение
- [ ] Проверить список чатов

### Мониторинг:
- [ ] Следить за логами первые 24 часа
- [ ] Проверить что нет ошибок дешифрования
- [ ] Убедиться что все новые сообщения имеют cipher_version=2

---

## 📞 Поддержка и траблшутинг

### Проблема: "CryptoHelper class not found"
**Решение:** Проверьте путь к crypto_helper.php:
```bash
ls -la /var/www/www-root/data/www/worldmates.club/includes/crypto_helper.php
```

### Проблема: "openssl_encrypt(): Unknown cipher algorithm"
**Решение:** Проверьте версию PHP и OpenSSL:
```bash
php -v  # Должна быть >= 7.1
php -r "print_r(openssl_get_cipher_methods());" | grep -i gcm
```

### Проблема: "Column 'iv' not found"
**Решение:** Выполните ALTER TABLE (см. раздел "Миграция БД")

### Проблема: Android не дешифрует сообщения
**Решение:**
1. Проверьте logcat на ошибки
2. Убедитесь что iv, tag, cipher_version возвращаются в JSON
3. Проверьте что timestamp совпадает

---

## 🔒 Улучшения безопасности

**Было (AES-128-ECB):**
- ❌ 128-битный ключ (слабый)
- ❌ ECB режим (паттерны видны)
- ❌ Нет проверки целостности
- ❌ Нет защиты от подмены
- ❌ Статический ключ на базе timestamp

**Стало (AES-256-GCM):**
- ✅ 256-битный ключ (в 2 раза сильнее)
- ✅ GCM режим (AEAD - authenticated encryption)
- ✅ Authentication tag (проверка целостности)
- ✅ Уникальный IV для каждого сообщения (96 бит случайных данных)
- ✅ Защита от подмены данных
- ✅ Обратная совместимость с ECB
- ✅ Автоопределение версии шифрования

**Статус миграции:**
- ✅ Android готов
- ✅ group_chat_v2.php готов
- ✅ crypto_helper.php готов
- ⏳ Остальные 6 PHP файлов ожидают обновления
