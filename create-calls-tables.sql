-- ============================================================================
-- WebRTC Calls Tables для WorldMates Messenger
-- Структура бази для аудіо/відео дзвінків
-- Оптимізовано для MariaDB 10.11.13
-- ============================================================================

-- Використовувати вашу базу даних
-- USE socialhub;

-- ============================================================================
-- 1. ТАБЛИЦЯ ДЛЯ ОСОБИСТИХ ДЗВІНКІВ (1-на-1)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `from_id` INT(11) NOT NULL COMMENT 'ID того, хто ініціював дзвінок',
  `to_id` INT(11) NOT NULL COMMENT 'ID одержувача дзвінка',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio' COMMENT 'audio або video',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, connected, ended, rejected, missed',
  `room_name` VARCHAR(100) NOT NULL COMMENT 'Унікальне ім\'я кімнати для WebRTC',
  `sdp_offer` LONGTEXT NULL COMMENT 'SDP offer від ініціатора',
  `sdp_answer` LONGTEXT NULL COMMENT 'SDP answer від одержувача',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',

  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_room_name` (`room_name`),
  KEY `idx_from_id` (`from_id`),
  KEY `idx_to_id` (`to_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Додаємо зовнішні ключі окремо (якщо таблиці wo_users існують)
-- FOREIGN KEY (`from_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE;
-- FOREIGN KEY (`to_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE;

-- ============================================================================
-- 2. ТАБЛИЦЯ ДЛЯ ГРУПОВИХ ДЗВІНКІВ
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_group_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `group_id` INT(11) NOT NULL,
  `initiated_by` INT(11) NOT NULL COMMENT 'ID того, хто ініціював груповий дзвінок',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, active, ended',
  `room_name` VARCHAR(100) NOT NULL COMMENT 'Унікальне ім\'я кімнати',
  `sdp_offer` LONGTEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `started_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',
  `max_participants` INT(11) NULL COMMENT 'Максимум учасників',

  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_group_room_name` (`room_name`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_initiated_by` (`initiated_by`),
  KEY `idx_gc_status` (`status`),
  KEY `idx_gc_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Додаємо зовнішні ключі окремо (якщо таблиці існують)
-- FOREIGN KEY (`group_id`) REFERENCES `wo_groups`(`id`) ON DELETE CASCADE;
-- FOREIGN KEY (`initiated_by`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE;

-- ============================================================================
-- 3. ТАБЛИЦЯ ДЛЯ УЧАСНИКІВ ГРУПОВИХ ДЗВІНКІВ
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_group_call_participants` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `group_call_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `sdp_answer` LONGTEXT NULL COMMENT 'SDP answer від конкретного користувача',
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `left_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість участі в секундах',

  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_participant` (`group_call_id`, `user_id`),
  KEY `idx_gcp_group_call` (`group_call_id`),
  KEY `idx_gcp_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Додаємо зовнішні ключі окремо
-- FOREIGN KEY (`group_call_id`) REFERENCES `wo_group_calls`(`id`) ON DELETE CASCADE;
-- FOREIGN KEY (`user_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE;

-- ============================================================================
-- 4. ТАБЛИЦЯ ДЛЯ ICE CANDIDATES (для зберігання)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_ice_candidates` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `room_name` VARCHAR(100) NOT NULL,
  `candidate` LONGTEXT NOT NULL,
  `sdp_m_line_index` INT(11) NOT NULL,
  `sdp_mid` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_ice_room_name` (`room_name`),
  KEY `idx_ice_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. ТАБЛИЦЯ ДЛЯ СТАТИСТИКИ ДЗВІНКІВ
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_call_statistics` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `call_id` INT(11) NULL COMMENT 'Посилання на wo_calls',
  `group_call_id` INT(11) NULL COMMENT 'Посилання на wo_group_calls',
  `user_id` INT(11) NOT NULL,
  `call_type` VARCHAR(32) NOT NULL,
  `duration` INT(11) NOT NULL COMMENT 'Тривалість в секундах',
  `bandwidth_used` FLOAT NULL COMMENT 'Пропускна спроможність в МБ',
  `packet_loss` FLOAT NULL COMMENT 'Втрата пакетів в %',
  `average_latency` INT(11) NULL COMMENT 'Середня затримка в мс',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_stats_user` (`user_id`),
  KEY `idx_stats_call_type` (`call_type`),
  KEY `idx_stats_created_at` (`created_at`),
  KEY `idx_stats_call_id` (`call_id`),
  KEY `idx_stats_group_call_id` (`group_call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. ДОДАТКОВІ ІНДЕКСИ ДЛЯ ОПТИМІЗАЦІЇ (створюються якщо не існують)
-- ============================================================================

-- Перевірка існування індексів та створення
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
               AND table_name = 'wo_calls'
               AND index_name = 'idx_calls_user');
SET @sqlstmt := IF(@exist = 0,
    'CREATE INDEX idx_calls_user ON wo_calls(from_id, created_at DESC)',
    'SELECT "Index idx_calls_user already exists"');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
               AND table_name = 'wo_calls'
               AND index_name = 'idx_calls_to_user');
SET @sqlstmt := IF(@exist = 0,
    'CREATE INDEX idx_calls_to_user ON wo_calls(to_id, created_at DESC)',
    'SELECT "Index idx_calls_to_user already exists"');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
               AND table_name = 'wo_group_calls'
               AND index_name = 'idx_group_calls_group');
SET @sqlstmt := IF(@exist = 0,
    'CREATE INDEX idx_group_calls_group ON wo_group_calls(group_id, created_at DESC)',
    'SELECT "Index idx_group_calls_group already exists"');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
               AND table_name = 'wo_group_calls'
               AND index_name = 'idx_group_calls_initiator');
SET @sqlstmt := IF(@exist = 0,
    'CREATE INDEX idx_group_calls_initiator ON wo_group_calls(initiated_by, created_at DESC)',
    'SELECT "Index idx_group_calls_initiator already exists"');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- ІНФОРМАЦІЯ ПРО СТВОРЕНІ ТАБЛИЦІ
-- ============================================================================
SELECT 'Таблиці для WebRTC дзвінків успішно створені!' as 'Статус';
SELECT
    TABLE_NAME as 'Таблиця',
    CREATE_TIME as 'Час створення',
    TABLE_ROWS as 'Кількість рядків',
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) as 'Розмір (MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('wo_calls', 'wo_group_calls', 'wo_group_call_participants',
                     'wo_ice_candidates', 'wo_call_statistics')
ORDER BY TABLE_NAME;

-- ============================================================================
-- ПРИКЛАДИ ЗАПИТІВ
-- ============================================================================

-- Отримати всі дзвінки користувача за останні 30 днів
/*
SELECT
    id,
    CASE WHEN from_id = 123 THEN 'Вихідний' ELSE 'Вхідний' END as direction,
    CASE WHEN from_id = 123 THEN to_id ELSE from_id END as contact_id,
    call_type,
    status,
    duration,
    created_at
FROM wo_calls
WHERE (from_id = 123 OR to_id = 123)
  AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY created_at DESC;
*/

-- Отримати статистику дзвінків користувача
/*
SELECT
    COUNT(*) as total_calls,
    SUM(CASE WHEN status = 'connected' THEN 1 ELSE 0 END) as completed_calls,
    SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_calls,
    SUM(CASE WHEN status = 'rejected' THEN 1 ELSE 0 END) as rejected_calls,
    AVG(duration) as avg_duration,
    SUM(duration) as total_duration
FROM wo_calls
WHERE (from_id = 123 OR to_id = 123)
  AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
*/

-- Отримати активні групові дзвінки
/*
SELECT
    gc.id,
    gc.group_id,
    gc.call_type,
    gc.room_name,
    gc.created_at,
    COUNT(gcp.id) as participants_count
FROM wo_group_calls gc
LEFT JOIN wo_group_call_participants gcp ON gc.id = gcp.group_call_id AND gcp.left_at IS NULL
WHERE gc.status IN ('ringing', 'active')
GROUP BY gc.id
ORDER BY gc.created_at DESC;
*/

-- Отримати історію участі користувача в групових дзвінках
/*
SELECT
    gcp.group_call_id,
    gc.group_id,
    gc.call_type,
    gcp.joined_at,
    gcp.left_at,
    gcp.duration,
    gc.status
FROM wo_group_call_participants gcp
JOIN wo_group_calls gc ON gcp.group_call_id = gc.id
WHERE gcp.user_id = 123
ORDER BY gcp.joined_at DESC
LIMIT 20;
*/

-- ============================================================================
-- ОЧИСТКА СТАРИХ ДАНИХ (опціонально, запускати вручну)
-- ============================================================================

-- Видалити ICE candidates старше 24 годин
/*
DELETE FROM wo_ice_candidates
WHERE created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR);
*/

-- Видалити статистику старше 90 днів
/*
DELETE FROM wo_call_statistics
WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
*/

-- ============================================================================
-- ПРИМІТКИ
-- ============================================================================
/*
1. Зовнішні ключі закоментовані - розкоментуйте їх якщо таблиці wo_users та wo_groups існують
2. Індекси створюються з перевіркою існування - можна запускати скрипт повторно
3. UNIQUE ключі на room_name забезпечують унікальність кімнат
4. Всі таблиці використовують utf8mb4 для підтримки емодзі
5. Для MariaDB 10.11.13 всі команди оптимізовані
*/
