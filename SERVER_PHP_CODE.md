# 🚀 Серверний PHP код для Adaptive Transport

Цей файл містить серверний код який потрібно додати на сервер `worldmates.club`.

## 📁 Структура файлів на сервері

```
/var/www/worldmates.club/api/v2/
├── ping.php                          # NEW: Endpoint для перевірки латентності
├── endpoints/
│   ├── get_messages_v3.php          # NEW: Оновлений endpoint з load_mode
│   └── generate_thumbnail.php       # NEW: Генератор превью
└── helpers/
    └── ThumbnailGenerator.php       # NEW: Клас для створення превью
```

---

## 1️⃣ ping.php - Endpoint для перевірки з'єднання

```php
<?php
/**
 * 📡 Ping endpoint для NetworkQualityMonitor
 * Призначення: швидка перевірка латентності
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Мінімальна відповідь для швидкого ping
echo json_encode([
    'status' => 'ok',
    'timestamp' => time(),
    'server_time' => microtime(true)
]);
?>
```

**Розміщення:** `/var/www/worldmates.club/api/v2/ping.php`

---

## 2️⃣ get_messages_v3.php - Оновлений endpoint з load_mode

```php
<?php
/**
 * 📦 Get Messages V3 - З підтримкою adaptive loading
 *
 * Нові параметри:
 * - load_mode: "text_only", "with_thumbnails", "full"
 * - after_message_id: для HTTP polling (отримати повідомлення після ID)
 */

require_once '../config.php';
require_once '../helpers/ThumbnailGenerator.php';

header('Content-Type: application/json');

// Перевірка access_token
$access_token = $_GET['access_token'] ?? '';
if (empty($access_token)) {
    echo json_encode(['api_status' => 400, 'error' => 'No access token']);
    exit;
}

// Отримуємо параметри
$recipient_id = intval($_POST['recipient_id'] ?? 0);
$limit = intval($_POST['limit'] ?? 30);
$before_message_id = intval($_POST['before_message_id'] ?? 0);
$after_message_id = intval($_POST['after_message_id'] ?? 0); // NEW
$load_mode = $_POST['load_mode'] ?? 'full'; // NEW: text_only, with_thumbnails, full

// Валідація load_mode
$allowed_modes = ['text_only', 'with_thumbnails', 'full'];
if (!in_array($load_mode, $allowed_modes)) {
    $load_mode = 'full';
}

// Отримуємо userId з access_token
$user_id = getUserIdFromToken($access_token, $db);
if (!$user_id) {
    echo json_encode(['api_status' => 401, 'error' => 'Invalid token']);
    exit;
}

// Формуємо SQL запит
$sql = "SELECT * FROM messages WHERE ";

if ($recipient_id > 0) {
    // Отримати повідомлення для конкретного чату
    $sql .= "(from_id = ? AND to_id = ?) OR (from_id = ? AND to_id = ?)";
    $params = [$user_id, $recipient_id, $recipient_id, $user_id];
} else {
    // Отримати всі повідомлення користувача (для polling)
    $sql .= "to_id = ?";
    $params = [$user_id];
}

// Фільтр по message_id
if ($before_message_id > 0) {
    $sql .= " AND id < ?";
    $params[] = $before_message_id;
} elseif ($after_message_id > 0) {
    $sql .= " AND id > ?";
    $params[] = $after_message_id;
}

$sql .= " ORDER BY id DESC LIMIT ?";
$params[] = $limit;

// Виконуємо запит
$stmt = $db->prepare($sql);
$stmt->execute($params);
$messages = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Обробляємо повідомлення в залежності від load_mode
$processed_messages = [];

foreach ($messages as $msg) {
    $message = [
        'id' => intval($msg['id']),
        'from_id' => intval($msg['from_id']),
        'to_id' => intval($msg['to_id']),
        'text' => $msg['text'],
        'time' => intval($msg['time']),
        'seen' => intval($msg['seen']),
        'deleted_one' => intval($msg['deleted_one']),
        'deleted_two' => intval($msg['deleted_two']),
        'type' => $msg['media'] ? 'media' : 'text'
    ];

    // Обробка медіа в залежності від режиму
    if ($msg['media']) {
        switch ($load_mode) {
            case 'text_only':
                // Не включаємо медіа взагалі
                $message['has_media'] = true;
                $message['media_type'] = $msg['mediaFileName'] ?
                    getMediaType($msg['mediaFileName']) : 'image';
                break;

            case 'with_thumbnails':
                // Включаємо тільки превью
                $thumbnail = ThumbnailGenerator::generateOrGet(
                    $msg['media'],
                    $msg['id'],
                    200, // ширина
                    200  // висота
                );
                $message['media_thumbnail'] = $thumbnail;
                $message['has_full_media'] = true;
                $message['media_type'] = getMediaType($msg['mediaFileName'] ?? '');
                break;

            case 'full':
            default:
                // Повне медіа
                $message['media'] = $msg['media'];
                $message['mediaFileName'] = $msg['mediaFileName'];
                $message['mediaFileNames'] = $msg['mediaFileNames'];
                break;
        }
    }

    // Інші поля (тільки якщо НЕ text_only)
    if ($load_mode !== 'text_only') {
        $message['reply_id'] = intval($msg['reply_id'] ?? 0);
        $message['story_id'] = intval($msg['story_id'] ?? 0);
        $message['broadcast_id'] = intval($msg['broadcast_id'] ?? 0);
        $message['forward'] = intval($msg['forward'] ?? 0);
        $message['position'] = $msg['position'] ?? '';
        $message['stickers'] = $msg['stickers'] ?? '';
        $message['product_id'] = intval($msg['product_id'] ?? 0);
    }

    $processed_messages[] = $message;
}

// Відповідь
echo json_encode([
    'api_status' => 200,
    'messages' => array_reverse($processed_messages), // Сортуємо від старих до нових
    'load_mode' => $load_mode,
    'count' => count($processed_messages)
]);

// ==================== ДОПОМІЖНІ ФУНКЦІЇ ====================

function getUserIdFromToken($token, $db) {
    $stmt = $db->prepare("SELECT user_id FROM wo_sessions WHERE session_id = ? LIMIT 1");
    $stmt->execute([$token]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    return $row ? intval($row['user_id']) : null;
}

function getMediaType($filename) {
    $ext = strtolower(pathinfo($filename, PATHINFO_EXTENSION));

    $image_exts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'];
    $video_exts = ['mp4', 'webm', 'mov', 'avi', 'mkv'];
    $audio_exts = ['mp3', 'wav', 'ogg', 'm4a', 'aac'];

    if (in_array($ext, $image_exts)) return 'image';
    if (in_array($ext, $video_exts)) return 'video';
    if (in_array($ext, $audio_exts)) return 'audio';

    return 'file';
}
?>
```

**Розміщення:** `/var/www/worldmates.club/api/v2/endpoints/get_messages_v3.php`

---

## 3️⃣ ThumbnailGenerator.php - Генератор превью

```php
<?php
/**
 * 🖼️ ThumbnailGenerator - Клас для створення превью зображень та відео
 */

class ThumbnailGenerator {

    private static $thumbnail_cache_dir = __DIR__ . '/../cache/thumbnails/';

    /**
     * Згенерувати або отримати існуюче превью
     *
     * @param string $media_url Повний URL медіа
     * @param int $message_id ID повідомлення
     * @param int $width Ширина превью
     * @param int $height Висота превью
     * @return string URL превью
     */
    public static function generateOrGet($media_url, $message_id, $width = 200, $height = 200) {
        // Створюємо папку для кешу якщо її немає
        if (!is_dir(self::$thumbnail_cache_dir)) {
            mkdir(self::$thumbnail_cache_dir, 0755, true);
        }

        // Перевіряємо чи вже є превью в кеші
        $thumbnail_filename = "thumb_{$message_id}_{$width}x{$height}.jpg";
        $thumbnail_path = self::$thumbnail_cache_dir . $thumbnail_filename;

        if (file_exists($thumbnail_path)) {
            // Повертаємо URL кешованого превью
            return str_replace($_SERVER['DOCUMENT_ROOT'], '', $thumbnail_path);
        }

        // Визначаємо тип медіа
        $media_type = self::getMediaType($media_url);

        if ($media_type === 'image') {
            return self::generateImageThumbnail($media_url, $thumbnail_path, $width, $height);
        } elseif ($media_type === 'video') {
            return self::generateVideoThumbnail($media_url, $thumbnail_path, $width, $height);
        }

        // Якщо не вдалось - повертаємо placeholder
        return '/assets/images/placeholder.jpg';
    }

    /**
     * Згенерувати превью зображення
     */
    private static function generateImageThumbnail($source_url, $dest_path, $width, $height) {
        try {
            // Завантажуємо оригінальне зображення
            $source_path = $_SERVER['DOCUMENT_ROOT'] . parse_url($source_url, PHP_URL_PATH);

            if (!file_exists($source_path)) {
                return null;
            }

            // Визначаємо тип зображення
            $image_info = getimagesize($source_path);
            $mime_type = $image_info['mime'];

            // Створюємо resource з оригіналу
            switch ($mime_type) {
                case 'image/jpeg':
                    $source = imagecreatefromjpeg($source_path);
                    break;
                case 'image/png':
                    $source = imagecreatefrompng($source_path);
                    break;
                case 'image/gif':
                    $source = imagecreatefromgif($source_path);
                    break;
                case 'image/webp':
                    $source = imagecreatefromwebp($source_path);
                    break;
                default:
                    return null;
            }

            // Отримуємо розміри оригіналу
            $orig_width = imagesx($source);
            $orig_height = imagesy($source);

            // Обчислюємо пропорції
            $ratio = min($width / $orig_width, $height / $orig_height);
            $new_width = intval($orig_width * $ratio);
            $new_height = intval($orig_height * $ratio);

            // Створюємо превью
            $thumbnail = imagecreatetruecolor($new_width, $new_height);
            imagecopyresampled(
                $thumbnail, $source,
                0, 0, 0, 0,
                $new_width, $new_height,
                $orig_width, $orig_height
            );

            // Зберігаємо як JPEG (найкращий баланс якості/розміру)
            imagejpeg($thumbnail, $dest_path, 75);

            // Очищуємо пам'ять
            imagedestroy($source);
            imagedestroy($thumbnail);

            // Повертаємо URL
            return str_replace($_SERVER['DOCUMENT_ROOT'], '', $dest_path);

        } catch (Exception $e) {
            error_log("Thumbnail generation error: " . $e->getMessage());
            return null;
        }
    }

    /**
     * Згенерувати превью відео (перший кадр)
     */
    private static function generateVideoThumbnail($source_url, $dest_path, $width, $height) {
        try {
            $source_path = $_SERVER['DOCUMENT_ROOT'] . parse_url($source_url, PHP_URL_PATH);

            if (!file_exists($source_path)) {
                return null;
            }

            // Використовуємо FFmpeg для отримання першого кадру
            $ffmpeg_path = '/usr/bin/ffmpeg'; // Шлях до FFmpeg на сервері

            if (!file_exists($ffmpeg_path)) {
                error_log("FFmpeg not found");
                return null;
            }

            // Команда для створення превью
            $cmd = sprintf(
                '%s -i %s -vframes 1 -vf "scale=%d:%d:force_original_aspect_ratio=decrease" %s 2>&1',
                $ffmpeg_path,
                escapeshellarg($source_path),
                $width,
                $height,
                escapeshellarg($dest_path)
            );

            exec($cmd, $output, $return_var);

            if ($return_var === 0 && file_exists($dest_path)) {
                return str_replace($_SERVER['DOCUMENT_ROOT'], '', $dest_path);
            }

            return null;

        } catch (Exception $e) {
            error_log("Video thumbnail error: " . $e->getMessage());
            return null;
        }
    }

    /**
     * Визначити тип медіа
     */
    private static function getMediaType($url) {
        $ext = strtolower(pathinfo($url, PATHINFO_EXTENSION));

        $image_exts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'];
        $video_exts = ['mp4', 'webm', 'mov', 'avi', 'mkv'];

        if (in_array($ext, $image_exts)) return 'image';
        if (in_array($ext, $video_exts)) return 'video';

        return 'unknown';
    }

    /**
     * Очистити старий кеш превью (старіше 30 днів)
     */
    public static function cleanupOldCache($days = 30) {
        $files = glob(self::$thumbnail_cache_dir . '*');
        $now = time();

        foreach ($files as $file) {
            if (is_file($file)) {
                if ($now - filemtime($file) >= 60 * 60 * 24 * $days) {
                    unlink($file);
                }
            }
        }
    }
}
?>
```

**Розміщення:** `/var/www/worldmates.club/api/v2/helpers/ThumbnailGenerator.php`

---

## 4️⃣ Оновлення існуючого get-messages.php

Додайте ці рядки в існуючий `/api/v2/chat.php?type=get-messages`:

```php
// В секції отримання параметрів додайте:
$load_mode = Wo_Secure($_POST['load_mode'] ?? 'full');
$after_message_id = Wo_Secure($_POST['after_message_id'] ?? 0);

// В секції формування SQL:
if ($after_message_id > 0) {
    $query .= " AND id > {$after_message_id}";
}

// В секції обробки результатів:
if ($load_mode === 'text_only' && !empty($message['media'])) {
    unset($message['media']);
    $message['has_media'] = true;
}
```

---

## 📋 Інструкції з встановлення

### 1. Завантажити файли на сервер

```bash
# SSH на сервер
ssh root@worldmates.club

# Створити директорії
mkdir -p /var/www/worldmates.club/api/v2/cache/thumbnails
chmod 755 /var/www/worldmates.club/api/v2/cache/thumbnails

# Завантажити файли (використовуйте FTP або scp)
# Або створіть файли через nano/vim
```

### 2. Встановити залежності

```bash
# FFmpeg для створення превью відео
apt-get update
apt-get install ffmpeg -y

# Перевірка
ffmpeg -version
```

### 3. Налаштувати права доступу

```bash
chown -R www-data:www-data /var/www/worldmates.club/api/v2/cache
chmod -R 755 /var/www/worldmates.club/api/v2/cache
```

### 4. Налаштувати cron для очистки старих превью

```bash
# Редагувати crontab
crontab -e

# Додати рядок (очистка кожен день о 3:00)
0 3 * * * php /var/www/worldmates.club/api/v2/helpers/cleanup_thumbnails.php
```

### 5. Створити cleanup_thumbnails.php

```php
<?php
require_once 'ThumbnailGenerator.php';
ThumbnailGenerator::cleanupOldCache(30); // Видалити превью старіше 30 днів
echo "Cleanup completed\n";
?>
```

---

## 🧪 Тестування

### Тест 1: Перевірка ping endpoint

```bash
curl https://worldmates.club/api/v2/ping.php
# Очікуємо: {"status":"ok","timestamp":...}
```

### Тест 2: Отримання повідомлень (text-only)

```bash
curl -X POST "https://worldmates.club/api/v2/endpoints/get_messages_v3.php?access_token=YOUR_TOKEN" \
  -d "recipient_id=123&load_mode=text_only&limit=10"
```

### Тест 3: Отримання повідомлень з превью

```bash
curl -X POST "https://worldmates.club/api/v2/endpoints/get_messages_v3.php?access_token=YOUR_TOKEN" \
  -d "recipient_id=123&load_mode=with_thumbnails&limit=10"
```

---

## 📊 Моніторинг продуктивності

### Логування в Apache

Додайте в `/etc/apache2/sites-available/worldmates.conf`:

```apache
<Location /api/v2/endpoints/get_messages_v3.php>
    LogLevel info
    CustomLog ${APACHE_LOG_DIR}/adaptive_transport.log combined
</Location>
```

### Моніторинг розміру кешу

```bash
# Перевірити розмір кешу превью
du -sh /var/www/worldmates.club/api/v2/cache/thumbnails

# Кількість файлів
ls -1 /var/www/worldmates.club/api/v2/cache/thumbnails | wc -l
```

---

## 🔧 Налаштування продуктивності

### PHP-FPM

В `/etc/php/8.1/fpm/pool.d/www.conf`:

```ini
pm.max_children = 50
pm.start_servers = 10
pm.min_spare_servers = 5
pm.max_spare_servers = 15
```

### Nginx (якщо використовується)

```nginx
location /api/v2/cache/thumbnails/ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

---

## ✅ Чеклист розгортання

- [ ] ping.php створено та працює
- [ ] get_messages_v3.php створено
- [ ] ThumbnailGenerator.php створено
- [ ] Директорія cache/thumbnails створена з правильними правами
- [ ] FFmpeg встановлено
- [ ] Cron job для очистки налаштовано
- [ ] Тести пройдені успішно
- [ ] Логування налаштовано

---

## 🚨 Troubleshooting

### Проблема: Превью не генеруються

```bash
# Перевірити права доступу
ls -la /var/www/worldmates.club/api/v2/cache/thumbnails

# Перевірити логи помилок
tail -f /var/log/apache2/error.log

# Перевірити наявність GD
php -m | grep gd
```

### Проблема: Відео превью не працюють

```bash
# Перевірити FFmpeg
which ffmpeg
ffmpeg -version

# Тест генерації
ffmpeg -i /path/to/video.mp4 -vframes 1 -vf "scale=200:200" test_thumb.jpg
```

---

## 📞 Підтримка

Якщо виникають проблеми:
1. Перевірте логи: `/var/log/apache2/error.log`
2. Перевірте PHP error log: `/var/log/php8.1-fpm.log`
3. Увімкніть debug режим у PHP: `error_reporting = E_ALL`
