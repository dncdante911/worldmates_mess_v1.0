-- ============================================================================
-- WebRTC Calls Tables для WorldMates Messenger
-- Структура бази для аудіо/відео дзвінків
-- ============================================================================

USE socialhub;

-- ============================================================================
-- 1. ТАБЛИЦЯ ДЛЯ ОСОБИСТИХ ДЗВІНКІВ (1-на-1)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `from_id` INT(11) NOT NULL COMMENT 'ID того, хто ініціював дзвінок',
  `to_id` INT(11) NOT NULL COMMENT 'ID одержувача дзвінка',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio' COMMENT 'audio або video',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, connected, ended, rejected, missed',
  `room_name` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Унікальне ім\'я кімнати для WebRTC',
  `sdp_offer` LONGTEXT NULL COMMENT 'SDP offer від ініціатора',
  `sdp_answer` LONGTEXT NULL COMMENT 'SDP answer від одержувача',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',
  
  PRIMARY KEY (`id`),
  KEY `from_id` (`from_id`),
  KEY `to_id` (`to_id`),
  KEY `room_name` (`room_name`),
  KEY `status` (`status`),
  KEY `created_at` (`created_at`),
  FOREIGN KEY (`from_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`to_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. ТАБЛИЦЯ ДЛЯ ГРУПОВИХ ДЗВІНКІВ
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_group_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `group_id` INT(11) NOT NULL,
  `initiated_by` INT(11) NOT NULL COMMENT 'ID того, хто ініціював груповий дзвінок',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, active, ended',
  `room_name` VARCHAR(100) NOT NULL UNIQUE,
  `sdp_offer` LONGTEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `started_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',
  `max_participants` INT(11) NULL COMMENT 'Максимум учасників',
  
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `initiated_by` (`initiated_by`),
  KEY `room_name` (`room_name`),
  KEY `status` (`status`),
  FOREIGN KEY (`group_id`) REFERENCES `wo_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`initiated_by`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  KEY `group_call_id` (`group_call_id`),
  KEY `user_id` (`user_id`),
  UNIQUE KEY `unique_participant` (`group_call_id`, `user_id`),
  FOREIGN KEY (`group_call_id`) REFERENCES `wo_group_calls`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  KEY `room_name` (`room_name`),
  KEY `created_at` (`created_at`)
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
  KEY `user_id` (`user_id`),
  KEY `call_type` (`call_type`),
  KEY `created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. ІНДЕКСИ ДЛЯ ОПТИМІЗАЦІЇ ЗАПИТІВ
-- ============================================================================

-- Для знаходження всіх дзвінків користувача
CREATE INDEX idx_calls_user ON wo_calls(from_id, created_at DESC);
CREATE INDEX idx_calls_to_user ON wo_calls(to_id, created_at DESC);

-- Для групових дзвінків
CREATE INDEX idx_group_calls_group ON wo_group_calls(group_id, created_at DESC);
CREATE INDEX idx_group_calls_initiator ON wo_group_calls(initiated_by, created_at DESC);

-- ============================================================================
-- ПРИМЕРЫ ЗАПРОСІВ
-- ============================================================================

-- Отримати всі дзвінки користувача за останні 30 днів
/*
SELECT * FROM wo_calls 
WHERE (from_id = 123 OR to_id = 123) 
  AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY created_at DESC;
*/

-- Отримати статистику дзвінків
/*
SELECT 
  COUNT(*) as total_calls,
  SUM(CASE WHEN status = 'connected' THEN 1 ELSE 0 END) as completed_calls,
  SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_calls,
  AVG(duration) as avg_duration
FROM wo_calls
WHERE from_id = 123 AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
*/

-- Отримати активні групові дзвінки
/*
SELECT * FROM wo_group_calls 
WHERE status IN ('ringing', 'active')
ORDER BY created_at DESC;
*/

EOF
cat /home/claude/create-calls-tables.sql
Output

-- ============================================================================
-- WebRTC Calls Tables для WorldMates Messenger
-- Структура бази для аудіо/відео дзвінків
-- ============================================================================

USE socialhub;

-- ============================================================================
-- 1. ТАБЛИЦЯ ДЛЯ ОСОБИСТИХ ДЗВІНКІВ (1-на-1)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `from_id` INT(11) NOT NULL COMMENT 'ID того, хто ініціював дзвінок',
  `to_id` INT(11) NOT NULL COMMENT 'ID одержувача дзвінка',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio' COMMENT 'audio або video',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, connected, ended, rejected, missed',
  `room_name` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Унікальне ім\'я кімнати для WebRTC',
  `sdp_offer` LONGTEXT NULL COMMENT 'SDP offer від ініціатора',
  `sdp_answer` LONGTEXT NULL COMMENT 'SDP answer від одержувача',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',
  
  PRIMARY KEY (`id`),
  KEY `from_id` (`from_id`),
  KEY `to_id` (`to_id`),
  KEY `room_name` (`room_name`),
  KEY `status` (`status`),
  KEY `created_at` (`created_at`),
  FOREIGN KEY (`from_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`to_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. ТАБЛИЦЯ ДЛЯ ГРУПОВИХ ДЗВІНКІВ
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wo_group_calls` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `group_id` INT(11) NOT NULL,
  `initiated_by` INT(11) NOT NULL COMMENT 'ID того, хто ініціював груповий дзвінок',
  `call_type` VARCHAR(32) NOT NULL DEFAULT 'audio',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ringing' COMMENT 'ringing, active, ended',
  `room_name` VARCHAR(100) NOT NULL UNIQUE,
  `sdp_offer` LONGTEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `started_at` TIMESTAMP NULL,
  `ended_at` TIMESTAMP NULL,
  `duration` INT(11) NULL COMMENT 'Тривалість в секундах',
  `max_participants` INT(11) NULL COMMENT 'Максимум учасників',
  
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `initiated_by` (`initiated_by`),
  KEY `room_name` (`room_name`),
  KEY `status` (`status`),
  FOREIGN KEY (`group_id`) REFERENCES `wo_groups`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`initiated_by`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  KEY `group_call_id` (`group_call_id`),
  KEY `user_id` (`user_id`),
  UNIQUE KEY `unique_participant` (`group_call_id`, `user_id`),
  FOREIGN KEY (`group_call_id`) REFERENCES `wo_group_calls`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `wo_users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
  KEY `room_name` (`room_name`),
  KEY `created_at` (`created_at`)
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
  KEY `user_id` (`user_id`),
  KEY `call_type` (`call_type`),
  KEY `created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. ІНДЕКСИ ДЛЯ ОПТИМІЗАЦІЇ ЗАПИТІВ
-- ============================================================================

-- Для знаходження всіх дзвінків користувача
CREATE INDEX idx_calls_user ON wo_calls(from_id, created_at DESC);
CREATE INDEX idx_calls_to_user ON wo_calls(to_id, created_at DESC);

-- Для групових дзвінків
CREATE INDEX idx_group_calls_group ON wo_group_calls(group_id, created_at DESC);
CREATE INDEX idx_group_calls_initiator ON wo_group_calls(initiated_by, created_at DESC);

-- ============================================================================
-- ПРИМЕРЫ ЗАПРОСІВ
-- ============================================================================

-- Отримати всі дзвінки користувача за останні 30 днів
/*
SELECT * FROM wo_calls 
WHERE (from_id = 123 OR to_id = 123) 
  AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY created_at DESC;
*/

-- Отримати статистику дзвінків
/*
SELECT 
  COUNT(*) as total_calls,
  SUM(CASE WHEN status = 'connected' THEN 1 ELSE 0 END) as completed_calls,
  SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_calls,
  AVG(duration) as avg_duration
FROM wo_calls
WHERE from_id = 123 AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
*/

-- Отримати активні групові дзвінки
/*
SELECT * FROM wo_group_calls 
WHERE status IN ('ringing', 'active')
ORDER BY created_at DESC;
*/