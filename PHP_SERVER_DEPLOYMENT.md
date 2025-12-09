# Инструкция по публикации PHP файлов на сервер WoWonder

## Где находятся PHP файлы для загрузки

Файлы находятся в папке: `php_server_files/`

```
worldmates_mess_v1.0/php_server_files/
├── upload_image.php
├── upload_video.php
├── upload_audio.php
├── upload_file.php
└── README.md
```

## Куда загружать на сервер

### 1. Найти директорию `xhr` на сервере

На сервере с WoWonder обычно структура такая:

```
/var/www/html/worldmates.club/
├── upload/              # Загруженные файлы
├── xhr/                 # <-- СЮДА загружаем наши PHP файлы
├── api/
├── config.php
└── ...
```

Или может быть:

```
/home/yourusername/public_html/
├── upload/
├── xhr/                 # <-- СЮДА загружаем
└── ...
```

### 2. Способы загрузки файлов

#### Способ 1: FTP/SFTP (FileZilla, WinSCP)

1. Подключитесь к серверу через FTP/SFTP
2. Найдите папку `xhr/`
3. Загрузите туда файлы:
   - `upload_image.php`
   - `upload_video.php`
   - `upload_audio.php`
   - `upload_file.php`

#### Способ 2: SSH/SCP

```bash
# С вашего компьютера
cd worldmates_mess_v1.0/php_server_files

# Загрузите файлы на сервер
scp upload_image.php user@worldmates.club:/var/www/html/worldmates.club/xhr/
scp upload_video.php user@worldmates.club:/var/www/html/worldmates.club/xhr/
scp upload_audio.php user@worldmates.club:/var/www/html/worldmates.club/xhr/
scp upload_file.php user@worldmates.club:/var/www/html/worldmates.club/xhr/
```

#### Способ 3: cPanel File Manager

1. Войдите в cPanel
2. Откройте File Manager
3. Найдите папку `public_html/xhr/`
4. Нажмите Upload
5. Загрузите все 4 PHP файла

### 3. Установить правильные права доступа

После загрузки файлов установите права:

```bash
# Подключитесь по SSH
ssh user@worldmates.club

# Перейдите в директорию xhr
cd /var/www/html/worldmates.club/xhr

# Установите права на файлы
chmod 644 upload_image.php
chmod 644 upload_video.php
chmod 644 upload_audio.php
chmod 644 upload_file.php

# Убедитесь, что владелец - веб-сервер
chown www-data:www-data upload_*.php

# Или если используете Apache
chown apache:apache upload_*.php
```

### 4. Проверить директории для загрузки

Убедитесь, что существуют папки для загрузки:

```bash
ls -la /var/www/html/worldmates.club/upload/
```

Должны быть папки:
- `photos/` - для изображений
- `videos/` - для видео
- `sounds/` - для аудио
- `files/` - для файлов

Если их нет, создайте:

```bash
cd /var/www/html/worldmates.club/upload
mkdir -p photos videos sounds files
chmod 755 photos videos sounds files
chown www-data:www-data photos videos sounds files
```

### 5. Интеграция с существующей системой WoWonder

#### Вариант A: Замена существующих файлов (рекомендуется)

Если у вас уже есть `upload_image.php` в `/xhr/`, замените его новым:

```bash
cd /var/www/html/worldmates.club/xhr
mv upload_image.php upload_image.php.old  # Backup
# Загрузите новый файл
```

#### Вариант B: Создание отдельных endpoints

Если хотите сохранить старые файлы, переименуйте новые:

```bash
# На сервере
cd /var/www/html/worldmates.club/xhr
mv upload_image.php upload_image_v2.php
mv upload_video.php upload_video_v2.php
# ...
```

Затем в Android приложении измените endpoints в `WorldMatesApi.kt`:

```kotlin
@Multipart
@POST("/xhr/upload_image_v2.php")  // Изменено
suspend fun uploadImage(...)
```

### 6. Настройка PHP

Проверьте `php.ini` на сервере:

```bash
# Найдите php.ini
php --ini

# Или
locate php.ini
```

Убедитесь, что установлены правильные лимиты:

```ini
upload_max_filesize = 1G
post_max_size = 1G
max_execution_time = 300
memory_limit = 512M
max_input_time = 300
```

После изменений перезагрузите PHP:

```bash
# Для PHP-FPM
sudo systemctl restart php8.1-fpm

# Для Apache
sudo systemctl restart apache2

# Для Nginx
sudo systemctl restart nginx
sudo systemctl restart php-fpm
```

### 7. Проверка работы

#### Проверка через браузер

Откройте в браузере:
```
https://worldmates.club/xhr/upload_image.php
```

Должна появиться ошибка (это нормально):
```json
{
  "status": 400,
  "error": "Method not allowed. Use POST."
}
```

Это значит, что файл работает!

#### Проверка через curl

```bash
curl -X POST \
  "https://worldmates.club/xhr/upload_image.php?access_token=YOUR_TOKEN&f=upload_image" \
  -F "server_key=YOUR_SERVER_KEY" \
  -F "image=@/path/to/test.jpg"
```

Ожидаемый ответ:
```json
{
  "status": 200,
  "image": "https://worldmates.club/upload/photos/2025/12/...",
  "image_src": "upload/photos/2025/12/...",
  ...
}
```

### 8. Логирование и отладка

Если что-то не работает, проверьте логи:

```bash
# PHP errors
tail -f /var/log/php_errors.log
tail -f /var/log/php8.1-fpm.log

# Nginx errors
tail -f /var/log/nginx/error.log

# Apache errors
tail -f /var/log/apache2/error.log

# Проверка загрузки файлов
ls -lah /var/www/html/worldmates.club/upload/photos/
```

### 9. Безопасность

1. **CSRF Protection**: Убедитесь, что `server_key` и `access_token` валидируются

2. **File Type Validation**: Код уже проверяет MIME-типы

3. **File Size Limits**: Установлены в коде

4. **.htaccess** (для Apache): Создайте в `/xhr/`:

```apache
# /xhr/.htaccess
<Files "upload_*.php">
    Order Deny,Allow
    Allow from all
</Files>

# Запретить прямой доступ к загруженным скриптам
<FilesMatch "\.(php|phtml)$">
    Order Deny,Allow
    Deny from all
</FilesMatch>
```

### 10. Troubleshooting

#### Проблема: 403 Forbidden

**Решение:**
```bash
chmod 644 upload_*.php
chown www-data:www-data upload_*.php
```

#### Проблема: Файлы не загружаются

**Проверьте:**
1. Права на папку `upload/` (должно быть 755)
2. PHP лимиты (`upload_max_filesize`, `post_max_size`)
3. Свободное место на диске: `df -h`

#### Проблема: Ошибка "Failed to create upload directory"

**Решение:**
```bash
mkdir -p /var/www/html/worldmates.club/upload/{photos,videos,sounds,files}
chmod 755 /var/www/html/worldmates.club/upload/*
chown -R www-data:www-data /var/www/html/worldmates.club/upload/
```

## Дополнительно: Автоматическая установка

Создайте скрипт `deploy_php.sh`:

```bash
#!/bin/bash

SERVER="user@worldmates.club"
REMOTE_PATH="/var/www/html/worldmates.club/xhr"
LOCAL_PATH="./php_server_files"

echo "Загрузка PHP файлов на сервер..."

scp $LOCAL_PATH/upload_image.php $SERVER:$REMOTE_PATH/
scp $LOCAL_PATH/upload_video.php $SERVER:$REMOTE_PATH/
scp $LOCAL_PATH/upload_audio.php $SERVER:$REMOTE_PATH/
scp $LOCAL_PATH/upload_file.php $SERVER:$REMOTE_PATH/

echo "Установка прав доступа..."

ssh $SERVER << 'EOF'
cd /var/www/html/worldmates.club/xhr
chmod 644 upload_*.php
chown www-data:www-data upload_*.php
echo "Готово!"
EOF
```

Запуск:
```bash
chmod +x deploy_php.sh
./deploy_php.sh
```

## Проверка после установки

1. Откройте Android приложение
2. Отправьте фото в чат
3. Проверьте logcat:
   ```bash
   adb logcat | grep -i "MediaUploader\|API_LOG"
   ```
4. Должны увидеть:
   - `Статус завантаження: 200`
   - `ImageURL: https://worldmates.club/upload/photos/...`

Готово! PHP файлы установлены и работают.
