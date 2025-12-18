# 🔐 PHP Encryption Replacement Guide
## Замена openssl_encrypt() на CryptoHelper::encryptGCM()

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

### Локация: `/home/user/api-worldmates/get_chats.php`

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

## 📁 Файл 5: group_chat.php

### Локация: `/home/user/api-worldmates/group_chat.php`

Этот файл имеет **2 места** с шифрованием.

### Строка 761 - Сообщения группы - БЫЛО:

```php
foreach ($messages as $message) {
    $message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
    $message['org_text'] = $message['text'];
```

### Строка 761 - Сообщения группы - СТАЛО:

```php
foreach ($messages as $message) {
    // Шифруем текст сообщения группы с использованием AES-256-GCM
    $encrypted = CryptoHelper::encryptGCM($message['text'], $message['time']);
    if ($encrypted !== false) {
        $message['text'] = $encrypted['text'];
        $message['iv'] = $encrypted['iv'];
        $message['tag'] = $encrypted['tag'];
        $message['cipher_version'] = $encrypted['cipher_version'];
    }

    $message['org_text'] = $message['text'];
```

### Строка 898 - Last message групп - БЫЛО:

```php
$groups[$key]['last_message']['text'] = openssl_encrypt($groups[$key]['last_message']['text'], "AES-128-ECB", $groups[$key]['last_message']['time']);
```

### Строка 898 - Last message групп - СТАЛО:

```php
// Шифруем последнее сообщение группы с использованием AES-256-GCM
$encrypted = CryptoHelper::encryptGCM(
    $groups[$key]['last_message']['text'],
    $groups[$key]['last_message']['time']
);
if ($encrypted !== false) {
    $groups[$key]['last_message']['text'] = $encrypted['text'];
    $groups[$key]['last_message']['iv'] = $encrypted['iv'];
    $groups[$key]['last_message']['tag'] = $encrypted['tag'];
    $groups[$key]['last_message']['cipher_version'] = $encrypted['cipher_version'];
}
```

---

## 📁 Файл 6: get-site-settings.php

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

## 📁 Файл 7: phone/get_users_list.php

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

## 🔍 Как проверить что все работает

### 1. Проверка поддержки GCM на сервере:

Создайте временный файл `test_gcm.php`:

```php
<?php
require_once('includes/crypto_helper.php');

if (CryptoHelper::isGCMSupported()) {
    echo "✅ AES-GCM поддерживается!\n";

    // Тест шифрования
    $plaintext = "Hello World";
    $timestamp = time();

    $encrypted = CryptoHelper::encryptGCM($plaintext, $timestamp);
    if ($encrypted !== false) {
        echo "✅ Шифрование работает!\n";
        echo "Text: " . $encrypted['text'] . "\n";
        echo "IV: " . $encrypted['iv'] . "\n";
        echo "Tag: " . $encrypted['tag'] . "\n";
        echo "Version: " . $encrypted['cipher_version'] . "\n";

        // Тест дешифрования
        $decrypted = CryptoHelper::decryptGCM(
            $encrypted['text'],
            $encrypted['iv'],
            $encrypted['tag'],
            $timestamp
        );

        if ($decrypted === $plaintext) {
            echo "✅ Дешифрование работает!\n";
            echo "Decrypted: $decrypted\n";
        } else {
            echo "❌ Дешифрование НЕ работает!\n";
        }
    } else {
        echo "❌ Шифрование НЕ работает!\n";
    }
} else {
    echo "❌ AES-GCM НЕ поддерживается на этом сервере!\n";
    echo "Доступные методы: " . implode(', ', openssl_get_cipher_methods()) . "\n";
}
?>
```

Запустите: `php test_gcm.php`

### 2. Проверка логов:

После внедрения проверьте логи PHP:

```bash
tail -f /var/log/php_errors.log
```

Ищите сообщения:
- `CryptoHelper: GCM encryption failed` - ошибка шифрования
- `CryptoHelper: GCM decryption failed` - ошибка дешифрования

### 3. Проверка в Android приложении:

- Отправьте тестовое сообщение
- Проверьте logcat на наличие:
  - `🔐 Дешифрування для ...`
  - `Cipher version: 2`
  - `Has IV/TAG: true/true`

---

## ⚠️ Важные замечания

### 1. Обратная совместимость

Старые сообщения (без iv/tag) будут автоматически дешифровываться через ECB режим на Android.

### 2. Структура базы данных

⚠️ **КРИТИЧНО:** Если вы храните зашифрованные сообщения в базе данных, убедитесь что в таблице есть поля:
- `iv` VARCHAR(255)
- `tag` VARCHAR(255)
- `cipher_version` INT

Если полей нет, добавьте их:

```sql
ALTER TABLE Wo_Messages ADD COLUMN iv VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN tag VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN cipher_version INT DEFAULT 1;
```

### 3. Порядок внедрения

1. ✅ Сначала обновите Android приложение (уже сделано)
2. ✅ Затем обновите PHP файлы на сервере
3. ⏳ Выпустите обновление пользователям
4. 📊 Мониторьте логи на предмет ошибок

### 4. Rollback план

Если что-то пойдет не так, можно откатиться:

```php
// Временно вернуться на ECB
$message['text'] = openssl_encrypt($message['text'], "AES-128-ECB", $message['time']);
```

---

## 📊 Сводная таблица файлов

| Файл | Количество мест | Строки | Статус |
|------|----------------|--------|--------|
| send-message.php | 1 | 178 | ⏳ Ожидает |
| get_user_messages.php | 2 | 46, 98 | ⏳ Ожидает |
| get_chats.php | 3 | 84, 176, 262 | ⏳ Ожидает |
| page_chat.php | 2 | 245, 407 | ⏳ Ожидает |
| group_chat.php | 2 | 761, 898 | ⏳ Ожидает |
| get-site-settings.php | 1 | 50 | ⏳ Ожидает |
| phone/get_users_list.php | 1 | 162 | ⏳ Ожидает |

**ВСЕГО:** 12 мест в 7 файлах

---

## 🎯 Следующие шаги

1. Сделайте бэкап всех файлов перед изменениями
2. Проверьте что crypto_helper.php находится в `/includes/crypto_helper.php`
3. Замените код во всех 7 файлах по примерам выше
4. Запустите `test_gcm.php` для проверки
5. Протестируйте отправку/получение сообщений
6. Проверьте логи на ошибки
7. Проверьте что Android приложение корректно дешифрует новые сообщения

---

## 📞 Поддержка

Если возникли проблемы:
- Проверьте версию PHP (должна быть >= 7.1)
- Проверьте что OpenSSL расширение включено
- Проверьте логи PHP на ошибки
- Проверьте что поля iv/tag/cipher_version добавлены в БД (если храните там зашифрованные данные)

**Статус миграции:** ✅ Android готов | ⏳ PHP ожидает внедрения
