# 📦 Установка серверных файлов WorldMates

## 🎯 Обзор

Все PHP файлы находятся в папке `server_modifications/` основного репозитория.

---

## 📂 Файлы для установки:

### 1. crypto_helper.php
**Назначение:** AES-256-GCM шифрование на сервере
**Путь на сервере:** `/var/www/www-root/data/www/worldmates.club/api/v2/crypto_helper.php`

**Копирование:**
```bash
scp server_modifications/crypto_helper.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

---

### 2. send-message.php (гибридная версия)
**Назначение:** Отправка личных сообщений с поддержкой GCM/ECB
**Путь на сервере:** `/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/send-message.php`

**Копирование:**
```bash
scp server_modifications/send-message.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/
```

**Особенности:**
- ✅ Определение типа клиента по `use_gcm` параметру
- ✅ WorldMates → AES-256-GCM (cipher_version=2)
- ✅ WoWonder Official → AES-128-ECB (cipher_version=1)
- ✅ Шифрование ПЕРЕД сохранением в БД
- ✅ Возврат GCM полей (iv, tag, cipher_version)

---

### 3. get_user_messages.php (исправленная версия)
**Назначение:** Получение личных сообщений без повторной шифровки
**Путь на сервере:** `/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/get_user_messages.php`

**Копирование:**
```bash
scp server_modifications/get_user_messages.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/
```

**Особенности:**
- ✅ Убрана повторная шифровка (было 2 раза шифровалось!)
- ✅ Возвращает сообщения как есть из БД
- ✅ Поля iv, tag, cipher_version передаются в ответе
- ✅ Android сам расшифровывает на клиенте

---

### 4. group_chat_v2.php (гибридная версия)
**Назначение:** API для групповых чатов с поддержкой GCM/ECB
**Путь на сервере:** `/var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php`

**Копирование:**
```bash
scp server_modifications/group_chat_v2.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

**Особенности:**
- ✅ Собственный API, написанный с нуля
- ✅ Гибридное шифрование (GCM/ECB)
- ✅ Логирование в `/api/v2/logs/group_chat_v2.log`
- ✅ REST API эндпоинты

---

## 🚀 Быстрая установка (все файлы):

### Вариант 1: Из локального репозитория

```bash
# Переходим в папку с репозиторием
cd /home/user/worldmates_mess_v1.0/server_modifications

# Копируем crypto_helper
scp crypto_helper.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/

# Копируем endpoints
scp send-message.php get_user_messages.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

# Копируем group_chat
scp group_chat_v2.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

### Вариант 2: Одной командой

```bash
cd /home/user/worldmates_mess_v1.0 && \
scp server_modifications/crypto_helper.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/ && \
scp server_modifications/{send-message,get_user_messages}.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/ && \
scp server_modifications/group_chat_v2.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/
```

---

## ✅ Проверка установки:

### На сервере выполните:

```bash
# Проверка наличия файлов
ls -lh /var/www/www-root/data/www/worldmates.club/api/v2/crypto_helper.php
ls -lh /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/send-message.php
ls -lh /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/get_user_messages.php
ls -lh /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php

# Проверка прав доступа (должно быть readable)
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/crypto_helper.php
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/*.php
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php
```

---

## 🗄️ Подготовка базы данных:

Убедитесь что в таблице `Wo_Messages` есть поля для GCM:

```sql
-- Проверка структуры
DESCRIBE Wo_Messages;

-- Если полей нет, добавьте их:
ALTER TABLE Wo_Messages ADD COLUMN iv VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN tag VARCHAR(255) NULL;
ALTER TABLE Wo_Messages ADD COLUMN cipher_version INT DEFAULT 1;
```

---

## 🧪 Тестирование:

### 1. Проверка шифрования:

**Отправьте сообщение из WorldMates:**
```
"Test GCM 🔐"
```

**Проверьте в БД:**
```sql
SELECT
    id, from_id, to_id,
    SUBSTRING(text, 1, 30) as encrypted_text,
    iv, tag, cipher_version,
    FROM_UNIXTIME(time) as sent_time
FROM Wo_Messages
ORDER BY id DESC LIMIT 1;
```

**Ожидаемый результат:**
```
cipher_version = 2
iv = (base64 строка ~16 символов)
tag = (base64 строка ~24 символа)
text = (base64 зашифрованный текст)
```

### 2. Проверка расшифровки:

**В Android приложении:**
- Получатель должен видеть: "Test GCM 🔐" ✅ (расшифрованное)
- Не должно быть base64 строк в интерфейсе ✅

### 3. Проверка гибридного режима:

**Из официального WoWonder отправьте:**
```
"Test ECB"
```

**В БД:**
```sql
SELECT cipher_version, iv, tag FROM Wo_Messages ORDER BY id DESC LIMIT 1;
```

**Ожидаемый результат:**
```
cipher_version = 1
iv = NULL
tag = NULL
```

**В WorldMates:**
- Должно отображаться: "Test ECB" ✅ (DecryptionUtility поддерживает оба метода)

---

## 📊 Структура шифрования:

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | VARCHAR | Зашифрованный текст (base64) |
| `iv` | VARCHAR(255) | Initialization Vector для GCM (base64, 12 байт) |
| `tag` | VARCHAR(255) | Authentication Tag для GCM (base64, 16 байт) |
| `cipher_version` | INT | 1=ECB (legacy), 2=GCM (modern) |

---

## 🔧 Устранение проблем:

### Проблема: HTTP 500 при отправке

**Проверьте:**
1. Логи PHP: `/var/www/www-root/data/www/worldmates.club/api/v2/logs/php_errors.log`
2. Наличие crypto_helper.php
3. Права доступа к файлам

**Решение:**
```bash
# Проверка require_once путей
grep "require_once" /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/send-message.php

# Должно быть: require_once(__DIR__ . '/../crypto_helper.php');
```

### Проблема: Сообщения показываются зашифрованными

**Причины:**
1. ❌ get_user_messages.php не обновлён (повторная шифровка)
2. ❌ iv/tag не передаются в ответе API
3. ❌ Android app не обновлён

**Решение:**
1. Убедитесь что get_user_messages.php НЕ содержит `openssl_encrypt` в строках 46, 98
2. Проверьте логи Android: `adb logcat | grep Decryption`
3. Пересоберите APK с обновлённым DecryptionUtility

### Проблема: Группы не работают

**Проверьте:**
```bash
# Логи group_chat_v2.php
tail -50 /var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log

# Должны быть записи:
# "Message encrypted with GCM (WorldMates)" - для WorldMates
# "Message encrypted with ECB (WoWonder official)" - для WoWonder
```

---

## 📖 Дополнительная документация:

- `HYBRID_ENCRYPTION_GUIDE.md` - Полное руководство по гибридному шифрованию
- `CRYPTO_MIGRATION_GUIDE.md` - Техническая документация по миграции
- `PHP_ENCRYPTION_REPLACEMENT_GUIDE.md` - Детальное руководство для PHP

---

## 🎯 Итоговый чеклист:

- [ ] Скопирован crypto_helper.php в `/api/v2/`
- [ ] Скопирован send-message.php в `/api/v2/endpoints/`
- [ ] Скопирован get_user_messages.php в `/api/v2/endpoints/`
- [ ] Скопирован group_chat_v2.php в `/api/v2/`
- [ ] Проверены права доступа (chmod 644)
- [ ] Добавлены поля iv, tag, cipher_version в БД
- [ ] Протестирована отправка из WorldMates (GCM)
- [ ] Протестирована отправка из WoWonder (ECB)
- [ ] Проверено отображение в обоих приложениях
- [ ] Логи не содержат ошибок

---

## 🚀 Готово!

После установки всех файлов:

✅ **WorldMates** работает с AES-256-GCM (топовая защита)
✅ **WoWonder Official** работает с AES-128-ECB (совместимость)
✅ Оба приложения на одном сервере
✅ Гибридное шифрование работает автоматически

Наслаждайтесь безопасным общением! 🔐🎉
