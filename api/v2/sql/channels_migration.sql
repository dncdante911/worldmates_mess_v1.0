-- ============================================
-- WorldMates Messenger - Channels Migration
-- Адаптація існуючої БД для підтримки Telegram-style каналів
-- MariaDB 10.11.13
-- ============================================

-- УВАГА: Цей скрипт ДОПОВНЮЄ існуючу БД, не створює нові таблиці!
-- Використовуємо Wo_GroupChat з type='channel' для каналів

-- ============================================
-- 1. Додаємо нові поля до Wo_GroupChat для каналів
-- ============================================

-- Перевіряємо чи існує поле username
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND COLUMN_NAME = 'username');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD COLUMN `username` VARCHAR(100) UNIQUE DEFAULT NULL COMMENT "Унікальний @username для публічних каналів"',
    'SELECT "Column username already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Перевіряємо чи існує поле subscribers_count
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND COLUMN_NAME = 'subscribers_count');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD COLUMN `subscribers_count` INT UNSIGNED DEFAULT 0 COMMENT "Кількість підписників"',
    'SELECT "Column subscribers_count already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Перевіряємо чи існує поле posts_count
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND COLUMN_NAME = 'posts_count');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD COLUMN `posts_count` INT UNSIGNED DEFAULT 0 COMMENT "Кількість постів"',
    'SELECT "Column posts_count already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Перевіряємо чи існує поле is_verified
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND COLUMN_NAME = 'is_verified');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD COLUMN `is_verified` TINYINT(1) DEFAULT 0 COMMENT "Верифікований канал"',
    'SELECT "Column is_verified already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Перевіряємо чи існує поле category
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND COLUMN_NAME = 'category');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD COLUMN `category` VARCHAR(100) DEFAULT NULL COMMENT "Категорія каналу"',
    'SELECT "Column category already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Додаємо індекс на username
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND INDEX_NAME = 'idx_username');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD INDEX `idx_username` (`username`)',
    'SELECT "Index idx_username already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Додаємо індекс на type
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'Wo_GroupChat'
    AND INDEX_NAME = 'idx_type');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE `Wo_GroupChat` ADD INDEX `idx_type` (`type`)',
    'SELECT "Index idx_type already exists" AS Info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- ============================================
-- 2. Таблиця для коментарів до повідомлень (постів у каналах)
-- ============================================
CREATE TABLE IF NOT EXISTS `Wo_MessageComments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `message_id` INT(11) NOT NULL COMMENT 'ID повідомлення з Wo_Messages',
  `user_id` INT(11) NOT NULL,
  `text` TEXT NOT NULL,
  `time` INT(11) NOT NULL,
  `edited_time` INT(11) DEFAULT NULL,
  `reply_to_comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Відповідь на коментар',

  INDEX `idx_message_id` (`message_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_time` (`time`),
  INDEX `idx_reply_to` (`reply_to_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. Таблиця для реакцій на коментарі до повідомлень
-- ============================================
CREATE TABLE IF NOT EXISTS `Wo_MessageCommentReactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `comment_id` BIGINT UNSIGNED NOT NULL,
  `user_id` INT(11) NOT NULL,
  `reaction` VARCHAR(10) NOT NULL COMMENT 'Emoji реакції',
  `time` INT(11) NOT NULL,

  UNIQUE KEY `unique_reaction` (`comment_id`, `user_id`, `reaction`),
  INDEX `idx_comment_id` (`comment_id`),
  INDEX `idx_user_id` (`user_id`),
  FOREIGN KEY (`comment_id`) REFERENCES `Wo_MessageComments`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 4. Таблиця для статистики переглядів повідомлень
-- ============================================
CREATE TABLE IF NOT EXISTS `Wo_MessageViews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `message_id` INT(11) NOT NULL,
  `user_id` INT(11) DEFAULT NULL COMMENT 'NULL для анонімних переглядів',
  `time` INT(11) NOT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,

  INDEX `idx_message_id` (`message_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 5. Тригери для автоматичного оновлення лічильників
-- ============================================

-- Тригер для subscribers_count при підписці
DROP TRIGGER IF EXISTS `update_channel_subscribers_count_insert`;
DELIMITER $$
CREATE TRIGGER `update_channel_subscribers_count_insert`
AFTER INSERT ON `Wo_GroupChatUsers`
FOR EACH ROW
BEGIN
  DECLARE channel_type VARCHAR(50);

  -- Перевіряємо чи це канал
  SELECT `type` INTO channel_type FROM `Wo_GroupChat` WHERE `group_id` = NEW.`group_id`;

  IF channel_type = 'channel' THEN
    UPDATE `Wo_GroupChat`
    SET `subscribers_count` = IFNULL(`subscribers_count`, 0) + 1
    WHERE `group_id` = NEW.`group_id`;
  END IF;
END$$
DELIMITER ;

-- Тригер для subscribers_count при відписці
DROP TRIGGER IF EXISTS `update_channel_subscribers_count_delete`;
DELIMITER $$
CREATE TRIGGER `update_channel_subscribers_count_delete`
AFTER DELETE ON `Wo_GroupChatUsers`
FOR EACH ROW
BEGIN
  DECLARE channel_type VARCHAR(50);

  -- Перевіряємо чи це канал
  SELECT `type` INTO channel_type FROM `Wo_GroupChat` WHERE `group_id` = OLD.`group_id`;

  IF channel_type = 'channel' THEN
    UPDATE `Wo_GroupChat`
    SET `subscribers_count` = GREATEST(0, IFNULL(`subscribers_count`, 0) - 1)
    WHERE `group_id` = OLD.`group_id`;
  END IF;
END$$
DELIMITER ;

-- Тригер для posts_count при створенні повідомлення
DROP TRIGGER IF EXISTS `update_channel_posts_count_insert`;
DELIMITER $$
CREATE TRIGGER `update_channel_posts_count_insert`
AFTER INSERT ON `Wo_Messages`
FOR EACH ROW
BEGIN
  DECLARE channel_type VARCHAR(50);

  IF NEW.`group_id` > 0 THEN
    -- Перевіряємо чи це канал
    SELECT `type` INTO channel_type FROM `Wo_GroupChat` WHERE `group_id` = NEW.`group_id`;

    IF channel_type = 'channel' THEN
      UPDATE `Wo_GroupChat`
      SET `posts_count` = IFNULL(`posts_count`, 0) + 1
      WHERE `group_id` = NEW.`group_id`;
    END IF;
  END IF;
END$$
DELIMITER ;

-- Тригер для posts_count при видаленні повідомлення
DROP TRIGGER IF EXISTS `update_channel_posts_count_delete`;
DELIMITER $$
CREATE TRIGGER `update_channel_posts_count_delete`
AFTER DELETE ON `Wo_Messages`
FOR EACH ROW
BEGIN
  DECLARE channel_type VARCHAR(50);

  IF OLD.`group_id` > 0 THEN
    -- Перевіряємо чи це канал
    SELECT `type` INTO channel_type FROM `Wo_GroupChat` WHERE `group_id` = OLD.`group_id`;

    IF channel_type = 'channel' THEN
      UPDATE `Wo_GroupChat`
      SET `posts_count` = GREATEST(0, IFNULL(`posts_count`, 0) - 1)
      WHERE `group_id` = OLD.`group_id`;
    END IF;
  END IF;
END$$
DELIMITER ;

-- ============================================
-- 6. Налаштування каналів за замовчуванням (JSON у полі settings)
-- ============================================
-- Формат JSON для поля settings:
-- {
--   "allow_comments": true,
--   "allow_reactions": true,
--   "allow_shares": true,
--   "show_statistics": true,
--   "notify_subscribers_new_post": true,
--   "auto_delete_posts_days": null,
--   "signature_enabled": false,
--   "comments_moderation": false,
--   "slow_mode_seconds": 0
-- }

-- ============================================
-- ГОТОВО! Тепер можна використовувати:
-- - Wo_GroupChat з type='channel' для каналів
-- - Wo_GroupChatUsers для підписників (role: owner/admin/moderator/member)
-- - Wo_Messages з group_id для постів у каналах
-- - wo_reactions з message_id для реакцій на пости
-- - Wo_MessageComments для коментарів до постів
-- - Wo_MessageCommentReactions для реакцій на коментарі
-- - Wo_MessageViews для статистики переглядів
-- ============================================
