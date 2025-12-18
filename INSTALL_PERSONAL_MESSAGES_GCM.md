# 🔐 Установка AES-256-GCM для личных сообщений

## ✅ Что уже сделано:
- **group_chat_v2.php** - групповые сообщения ✅
- **send-message.php** - личные сообщения ✅ (нужно скопировать на сервер)

---

## 📦 Копирование файлов на сервер:

### 1. Скопируйте обновленный send-message.php:

```bash
scp /home/user/api-worldmates/send-message.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/
```

### 2. Убедитесь что crypto_helper.php на месте:

```bash
# На сервере проверьте:
ls -la /var/www/www-root/data/www/worldmates.club/includes/crypto_helper.php
```

---

## 🧪 Тестирование:

### 1. Отправьте личное сообщение через Android:
Откройте чат с любым пользователем и отправьте: "Test GCM personal 🔐"

### 2. Проверьте в БД:

```sql
SELECT
    id,
    from_id,
    to_id,
    SUBSTRING(text, 1, 30) as text_preview,
    SUBSTRING(iv, 1, 20) as iv_preview,
    SUBSTRING(tag, 1, 20) as tag_preview,
    cipher_version,
    FROM_UNIXTIME(time) as sent_time
FROM Wo_Messages
WHERE to_id != 0  -- Личные сообщения (не группы)
ORDER BY id DESC
LIMIT 5;
```

**Ожидаемый результат:**
- `cipher_version` = **2**
- `iv` = заполнено (не NULL)
- `tag` = заполнено (не NULL)

### 3. Проверьте Android логи:

```
adb logcat | grep -E "cipher|GCM|Decryption"
```

Должны увидеть:
```
D/DecryptionUtility: Попытка расшифровки: version=2, hasIV=true, hasTag=true
D/EncryptionUtility: Message decrypted successfully with AES-GCM
D/DecryptionUtility: GCM decryption successful
```

---

## ⚠️ Важно:

**Fallback механизм:**
Если crypto_helper.php недоступен, send-message.php автоматически вернется к старому AES-128-ECB. Это гарантирует что сообщения всегда будут работать, даже если что-то пойдет не так.

**Проверка работы:**
```bash
# На сервере:
php -r "require_once('/var/www/www-root/data/www/worldmates.club/includes/crypto_helper.php'); echo CryptoHelper::isGCMSupported() ? 'OK' : 'FAIL';"
```

Должно вывести: `OK`

---

## 📊 Текущий статус:

| Тип сообщений | Файл | Статус |
|---------------|------|--------|
| Групповые | group_chat_v2.php | ✅ Работает |
| Личные | send-message.php | ✅ Готов (нужно скопировать) |
| Получение личных | get_user_messages.php | ⏳ Следующий |
| Получение групповых | group_chat_v2.php | ✅ Работает |

---

## 🎯 Следующий шаг:

После установки send-message.php нужно обновить **get_user_messages.php**, чтобы он тоже возвращал сообщения с полями iv, tag, cipher_version (сейчас он шифрует заново при получении, что неправильно).

Но сначала протестируем send-message.php! 🚀
