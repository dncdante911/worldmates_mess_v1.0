<?php
/**
 * Конфігурація підключення до бази даних
 * для group_chat_v2.php API
 */

// Налаштування бази даних
define('DB_HOST', 'localhost');
define('DB_NAME', 'socialhub');
define('DB_USER', 'social');
define('DB_PASS', '3344Frzaq0607DmC157');
define('DB_CHARSET', 'utf8mb4');

// Налаштування PDO
define('PDO_OPTIONS', [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
]);

// Шлях до лог-файлу
define('LOG_FILE', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log');

// Timezone
date_default_timezone_set('Europe/Kyiv');
