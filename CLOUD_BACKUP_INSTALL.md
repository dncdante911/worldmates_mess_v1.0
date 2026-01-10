# 📦 Cloud Backup - Инструкция по установке PHP endpoints

## ⚠️ ВАЖНО! Без этих файлов Cloud Backup НЕ РАБОТАЕТ!

Все PHP endpoints возвращают **HTTP 500**, пока вы не скопируете обновленные файлы на сервер.

---

## 📋 Что нужно скопировать

### 1. PHP Endpoints (ОБЯЗАТЕЛЬНО!)

Скопируйте эти файлы из проекта на ваш сервер:

```bash
# Локальный проект → Сервер
api-server-files/api/v2/endpoints/export-user-data.php
  → /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/export-user-data.php

api-server-files/api/v2/endpoints/import-user-data.php
  → /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/import-user-data.php

api-server-files/api/v2/endpoints/list-backups.php
  → /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/list-backups.php

api-server-files/api/v2/endpoints/get-backup-statistics.php
  → /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/get-backup-statistics.php
```

### 2. Config файл (должен уже быть)

Проверьте что существует:
```
/var/www/www-root/data/www/worldmates.club/api/v2/config.php
```

Если его нет, скопируйте:
```bash
api-server-files/api/v2/config.php
  → /var/www/www-root/data/www/worldmates.club/api/v2/config.php
```

---

## 🚀 Способы копирования

### Вариант 1: FTP/SFTP (FileZilla, WinSCP и т.д.)

1. Подключитесь к серверу через FTP/SFTP
2. Перейдите в `/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/`
3. Загрузите 4 файла из `api-server-files/api/v2/endpoints/`
4. Убедитесь, что права доступа `644` или `755`

### Вариант 2: SSH + SCP

```bash
# С вашего компьютера (Windows/Linux/Mac)
scp api-server-files/api/v2/endpoints/export-user-data.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/endpoints/import-user-data.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/endpoints/list-backups.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

scp api-server-files/api/v2/endpoints/get-backup-statistics.php \
    user@worldmates.club:/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/
```

### Вариант 3: SSH + Прямое редактирование

```bash
# Подключитесь к серверу
ssh user@worldmates.club

# Перейдите в директорию
cd /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/

# Создайте/отредактируйте файлы
nano export-user-data.php
# Скопируйте содержимое из локального файла

nano import-user-data.php
# Скопируйте содержимое...

nano list-backups.php
# ...

nano get-backup-statistics.php
# ...
```

---

## 📂 Создайте папку для бэкапов

```bash
# На сервере
mkdir -p /var/www/www-root/data/www/worldmates.club/upload/backups
chmod 755 /var/www/www-root/data/www/worldmates.club/upload/backups
```

---

## ✅ Проверка установки

### 1. Проверьте что файлы существуют:

```bash
# На сервере
ls -la /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/ | grep backup

# Должны быть:
# -rw-r--r-- export-user-data.php
# -rw-r--r-- import-user-data.php
# -rw-r--r-- list-backups.php
# -rw-r--r-- get-backup-statistics.php
```

### 2. Проверьте права доступа:

```bash
# Если нужно, установите права
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/*.php
```

### 3. Проверьте через curl:

```bash
# Замените YOUR_ACCESS_TOKEN на ваш реальный токен
curl "https://worldmates.club/api/v2/endpoints/get-backup-statistics.php?access_token=YOUR_ACCESS_TOKEN"

# Должен вернуть JSON с api_status: 200
```

---

## 🔍 Что проверяет каждый файл

### export-user-data.php
- ✅ Проверяет `require_once(__DIR__ . '/../config.php')`
- ✅ Подключается к БД через PDO
- ✅ Валидирует access_token
- ✅ Создает JSON бэкап в `/upload/backups/user_{ID}/`

### import-user-data.php
- ✅ Восстанавливает данные из JSON
- ✅ Импортирует сообщения (пропускает дубликаты)

### list-backups.php
- ✅ Сканирует директорию `/upload/backups/user_{ID}/`
- ✅ Возвращает список .json файлов

### get-backup-statistics.php
- ✅ Считает количество сообщений в БД
- ✅ Вычисляет размер бэкапов
- ✅ Возвращает реальную статистику

---

## 🐛 Troubleshooting

### Ошибка: HTTP 500

**Причина:** Файл не найден или не может подключить config.php

**Решение:**
```bash
# Проверьте что файл существует
ls -la /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/export-user-data.php

# Проверьте что config.php существует
ls -la /var/www/www-root/data/www/worldmates.club/api/v2/config.php

# Проверьте логи PHP
tail -f /var/log/php-fpm/error.log
# или
tail -f /var/www/www-root/data/www/worldmates.club/api/v2/logs/php_errors.log
```

### Ошибка: Permission denied

**Решение:**
```bash
# Установите правильные права
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/*.php
chown www-data:www-data /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/*.php

# Для папки бэкапов
chmod 755 /var/www/www-root/data/www/worldmates.club/upload/backups
chown www-data:www-data /var/www/www-root/data/www/worldmates.club/upload/backups
```

### Ошибка: Database connection failed

**Решение:**
Проверьте настройки в `/var/www/www-root/data/www/worldmates.club/api/v2/config.php`:
```php
define('DB_HOST', 'localhost');
define('DB_NAME', 'socialhub');
define('DB_USER', 'social');
define('DB_PASS', '3344Frzaq0607DmC157');
```

---

## 🎉 После установки

1. Откройте приложение WorldMates
2. Перейдите: **Налаштування → Сховище та бэкап**
3. Вы должны увидеть:
   - ✅ Реальную статистику (количество сообщений, размер)
   - ✅ Кнопку "Створити бекап зараз" (работает!)
   - ✅ Список бэкапів (если есть)

---

## 📚 Структура после установки

```
/var/www/www-root/data/www/worldmates.club/
├── api/
│   └── v2/
│       ├── config.php  ← Подключение к БД
│       └── endpoints/
│           ├── export-user-data.php  ← Создание бэкапа
│           ├── import-user-data.php  ← Восстановление
│           ├── list-backups.php      ← Список бэкапов
│           └── get-backup-statistics.php  ← Статистика
└── upload/
    └── backups/  ← Папка для бэкапов
        ├── user_1/
        │   ├── backup_2026-01-10_14-30-45.json
        │   └── backup_2026-01-09_18-20-12.json
        └── user_2/
            └── backup_2026-01-10_15-45-23.json
```

---

**Нужна помощь?** Проверьте логи и напишите мне!
