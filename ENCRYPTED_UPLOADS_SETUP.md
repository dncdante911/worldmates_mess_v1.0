# Установка поддержки зашифрованных загрузок медиа

## Проблема

Когда пользователь отправляет медиа (фото, видео, аудио, документы) с веб-версии, на мобильном приложении они приходят зашифрованными. Сервер возвращал пустой ответ (0 bytes) на запросы загрузки.

## Решение

Созданы новые PHP скрипты для корректной обработки загрузок медиа с веб-версии.

## Установка на сервер

### Шаг 1: Копирование PHP файлов

Скопируйте файлы из директории `php_server_files/` на ваш сервер:

```bash
# На вашем локальном компьютере
cd worldmates_mess_v1.0/php_server_files

# Копируем на сервер
scp upload_image.php user@worldmates.club:/path/to/site/xhr/
scp upload_video.php user@worldmates.club:/path/to/site/xhr/
scp upload_audio.php user@worldmates.club:/path/to/site/xhr/
scp upload_file.php user@worldmates.club:/path/to/site/xhr/
```

Или через FTP/файловый менеджер:
- Загрузите все 4 PHP файла в директорию `/xhr/` на вашем сервере

### Шаг 2: Создание директорий для загрузок

На сервере создайте директории для загрузок (если они не существуют):

```bash
mkdir -p /path/to/site/upload/photos
mkdir -p /path/to/site/upload/videos
mkdir -p /path/to/site/upload/sounds
mkdir -p /path/to/site/upload/files

# Установите права доступа
chmod 755 /path/to/site/upload
chmod 755 /path/to/site/upload/photos
chmod 755 /path/to/site/upload/videos
chmod 755 /path/to/site/upload/sounds
chmod 755 /path/to/site/upload/files

# Дайте веб-серверу права на запись
chown -R www-data:www-data /path/to/site/upload
```

### Шаг 3: Настройка PHP

Убедитесь, что в `php.ini` установлены достаточные лимиты:

```ini
upload_max_filesize = 1G
post_max_size = 1G
max_execution_time = 300
memory_limit = 512M
```

Перезагрузите PHP-FPM или Apache после изменений:

```bash
# Для PHP-FPM
sudo systemctl restart php8.1-fpm

# Для Apache
sudo systemctl restart apache2
```

### Шаг 4: Проверка работы

#### Тест через curl

```bash
# Тест загрузки изображения
curl -X POST \
  "https://worldmates.club/xhr/upload_image.php?access_token=YOUR_TOKEN&f=upload_image" \
  -F "server_key=YOUR_SERVER_KEY" \
  -F "image=@/path/to/test.jpg"

# Ожидаемый ответ:
{
  "status": 200,
  "image": "https://worldmates.club/upload/photos/2025/12/encrypted_img_1733755550_abc123.jpg",
  "image_src": "upload/photos/2025/12/encrypted_img_1733755550_abc123.jpg",
  "encrypted": false,
  "timestamp": 1733755550,
  "size": 245678,
  "type": "image/jpeg"
}
```

#### Тест из приложения

1. Откройте приложение
2. Отправьте изображение/видео/документ в чат
3. Проверьте логи:

```bash
# Android logcat
adb logcat | grep -i "MediaUploader\|API_LOG"
```

Вы должны увидеть:
- `Статус завантаження: 200`
- `ImageURL: https://worldmates.club/upload/photos/...`
- `Файл завантажено на сервер`

## Интеграция с существующей системой (опционально)

Если у вас уже есть файлы `upload_image.php` и другие в `/xhr/`, и вы хотите интегрировать с ними:

### Вариант 1: Замена (рекомендуется)

Просто замените старые файлы новыми. Новые файлы являются standalone и не требуют зависимостей.

### Вариант 2: Интеграция

Если нужно сохранить существующий функционал, добавьте в начало каждого PHP файла:

```php
<?php
// Подключаем существующую конфигурацию (если нужно)
if (file_exists('../config.php')) {
    require_once('../config.php');
}

// Проверяем, что это XHR запрос для загрузки
if (isset($_GET['f']) && $_GET['f'] == 'upload_image') {
    // ... код из нового upload_image.php ...
}
```

## Как работает шифрование

### На веб-версии (отправка)

Веб-версия может шифровать текст сообщения перед отправкой:
```javascript
// PHP на сервере: openssl_encrypt($text, "AES-128-ECB", $timestamp)
```

### В приложении (получение)

Приложение автоматически расшифровывает сообщения:

1. `DecryptionUtility.decryptMessageOrOriginal()` - расшифровывает текст
2. `DecryptionUtility.decryptMediaUrl()` - расшифровывает URL медиа
3. Используется AES-128-ECB с timestamp как ключом

**Важно:** Медиа-файлы (сами файлы) НЕ шифруются, шифруется только URL в некоторых случаях.

## Troubleshooting

### Ошибка: "Failed to create upload directory"

Проблема с правами доступа. Выполните:
```bash
chmod 755 /path/to/site/upload
chown www-data:www-data /path/to/site/upload
```

### Ошибка: "File too large"

Увеличьте лимиты в `php.ini` и перезагрузите PHP.

### Ошибка: "Invalid file type"

Убедитесь, что MIME-type файла соответствует разрешенным:
- Изображения: JPEG, PNG, GIF, WEBP
- Видео: MP4, WEBM, OGG, AVI, MOV
- Аудио: MP3, WAV, OGG, AAC, M4A

### Сервер возвращает пустой ответ (0 bytes)

1. Проверьте, что новые PHP файлы загружены правильно
2. Проверьте права доступа на файлы: `chmod 644 /path/to/xhr/*.php`
3. Проверьте логи веб-сервера: `tail -f /var/log/nginx/error.log`
4. Убедитесь, что PHP не выдает ошибки: включите `display_errors = On` временно

### Логи показывают "Сервер прийняв файл, але не повернув URL"

Это означает, что:
1. Файл был загружен на сервер (HTTP 200)
2. Но ответ не содержит `image`, `video`, `audio` или `file` поля

Проверьте:
- Правильно ли работает JSON кодирование в PHP
- Нет ли ошибок в логах PHP

## Дополнительная информация

### Структура ответа API

```json
{
  "status": 200,                    // HTTP статус
  "image": "https://...",           // Полный URL (для изображений)
  "image_src": "upload/...",        // Относительный путь
  "encrypted": false,               // Флаг шифрования
  "timestamp": 1733755550,          // Unix timestamp
  "size": 245678,                   // Размер файла в байтах
  "type": "image/jpeg"              // MIME тип
}
```

### Endpoints

| Endpoint | Параметр файла | Ответ | Макс размер |
|----------|---------------|-------|-------------|
| `/xhr/upload_image.php` | `image` | `image`, `image_src` | 15MB |
| `/xhr/upload_video.php` | `video` | `video`, `video_src` | 1GB |
| `/xhr/upload_audio.php` | `audio` | `audio`, `audio_src` | 100MB |
| `/xhr/upload_file.php` | `file` | `file`, `file_src` | 500MB |

### Безопасность

Все файлы проверяются:
1. По MIME-типу (не только по расширению)
2. По размеру
3. Имена файлов санитизируются
4. Требуется аутентификация (access_token + server_key)

## Поддержка

Если возникли проблемы:
1. Проверьте логи: `adb logcat | grep MediaUploader`
2. Проверьте логи сервера
3. Убедитесь, что все файлы загружены на сервер
4. Проверьте права доступа на директории
