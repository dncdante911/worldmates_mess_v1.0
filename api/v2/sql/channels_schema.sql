-- ============================================
-- WorldMates Messenger - Channels Database Schema
-- MariaDB 10.11.13
-- ============================================

-- Таблиця каналів
CREATE TABLE IF NOT EXISTS `channels` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `username` VARCHAR(100) UNIQUE DEFAULT NULL COMMENT 'Унікальний username для публічних каналів (@channel_name)',
  `description` TEXT DEFAULT NULL,
  `avatar_url` VARCHAR(500) DEFAULT NULL,
  `owner_id` BIGINT UNSIGNED NOT NULL COMMENT 'ID власника каналу',
  `is_private` TINYINT(1) DEFAULT 0 COMMENT '0 = публічний, 1 = приватний',
  `is_verified` TINYINT(1) DEFAULT 0 COMMENT 'Верифікований канал',
  `subscribers_count` INT UNSIGNED DEFAULT 0,
  `posts_count` INT UNSIGNED DEFAULT 0,
  `category` VARCHAR(100) DEFAULT NULL COMMENT 'Категорія каналу',
  `created_time` BIGINT UNSIGNED NOT NULL COMMENT 'Unix timestamp',
  `updated_time` BIGINT UNSIGNED DEFAULT NULL,

  -- Налаштування каналу (JSON або окремі поля)
  `allow_comments` TINYINT(1) DEFAULT 1,
  `allow_reactions` TINYINT(1) DEFAULT 1,
  `allow_shares` TINYINT(1) DEFAULT 1,
  `show_statistics` TINYINT(1) DEFAULT 1,
  `notify_subscribers_new_post` TINYINT(1) DEFAULT 1,
  `auto_delete_posts_days` INT DEFAULT NULL COMMENT 'Авто-видалення постів через N днів (NULL = вимкнено)',
  `signature_enabled` TINYINT(1) DEFAULT 0 COMMENT 'Підпис автора поста',
  `comments_moderation` TINYINT(1) DEFAULT 0 COMMENT 'Модерація коментарів',
  `slow_mode_seconds` INT DEFAULT 0 COMMENT 'Slow mode для коментарів (0 = вимкнено)',

  INDEX `idx_owner_id` (`owner_id`),
  INDEX `idx_username` (`username`),
  INDEX `idx_is_private` (`is_private`),
  INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця підписників каналів
CREATE TABLE IF NOT EXISTS `channel_subscribers` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `subscribed_time` BIGINT UNSIGNED NOT NULL COMMENT 'Unix timestamp',
  `notifications_enabled` TINYINT(1) DEFAULT 1,

  UNIQUE KEY `unique_subscription` (`channel_id`, `user_id`),
  INDEX `idx_channel_id` (`channel_id`),
  INDEX `idx_user_id` (`user_id`),
  FOREIGN KEY (`channel_id`) REFERENCES `channels`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця адміністраторів каналів
CREATE TABLE IF NOT EXISTS `channel_admins` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role` ENUM('admin', 'moderator', 'editor') DEFAULT 'admin',
  `added_time` BIGINT UNSIGNED NOT NULL,
  `added_by` BIGINT UNSIGNED NOT NULL COMMENT 'Хто додав адміна',

  -- Права адміністратора
  `can_post` TINYINT(1) DEFAULT 1,
  `can_edit_posts` TINYINT(1) DEFAULT 1,
  `can_delete_posts` TINYINT(1) DEFAULT 1,
  `can_pin_posts` TINYINT(1) DEFAULT 1,
  `can_edit_info` TINYINT(1) DEFAULT 0,
  `can_add_admins` TINYINT(1) DEFAULT 0,
  `can_ban_users` TINYINT(1) DEFAULT 0,
  `can_manage_settings` TINYINT(1) DEFAULT 0,

  UNIQUE KEY `unique_admin` (`channel_id`, `user_id`),
  INDEX `idx_channel_id` (`channel_id`),
  INDEX `idx_user_id` (`user_id`),
  FOREIGN KEY (`channel_id`) REFERENCES `channels`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця постів у каналах
CREATE TABLE IF NOT EXISTS `channel_posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT UNSIGNED NOT NULL,
  `author_id` BIGINT UNSIGNED NOT NULL,
  `text` TEXT NOT NULL,
  `media` JSON DEFAULT NULL COMMENT 'Масив медіа [{url, type, mime_type, size, etc}]',
  `created_time` BIGINT UNSIGNED NOT NULL,
  `edited_time` BIGINT UNSIGNED DEFAULT NULL,
  `is_edited` TINYINT(1) DEFAULT 0,
  `is_pinned` TINYINT(1) DEFAULT 0,
  `views_count` INT UNSIGNED DEFAULT 0,
  `reactions_count` INT UNSIGNED DEFAULT 0,
  `comments_count` INT UNSIGNED DEFAULT 0,
  `shares_count` INT UNSIGNED DEFAULT 0,
  `is_comments_enabled` TINYINT(1) DEFAULT 1,

  -- Forwards (пересилання)
  `forward_from_channel_id` BIGINT UNSIGNED DEFAULT NULL,
  `forward_from_post_id` BIGINT UNSIGNED DEFAULT NULL,

  INDEX `idx_channel_id` (`channel_id`),
  INDEX `idx_author_id` (`author_id`),
  INDEX `idx_created_time` (`created_time`),
  INDEX `idx_is_pinned` (`is_pinned`),
  FOREIGN KEY (`channel_id`) REFERENCES `channels`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця коментарів до постів
CREATE TABLE IF NOT EXISTS `channel_comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `post_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `text` TEXT NOT NULL,
  `created_time` BIGINT UNSIGNED NOT NULL,
  `edited_time` BIGINT UNSIGNED DEFAULT NULL,
  `is_edited` TINYINT(1) DEFAULT 0,
  `reply_to_comment_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Відповідь на коментар',
  `reactions_count` INT UNSIGNED DEFAULT 0,

  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_created_time` (`created_time`),
  INDEX `idx_reply_to` (`reply_to_comment_id`),
  FOREIGN KEY (`post_id`) REFERENCES `channel_posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця реакцій на пости
CREATE TABLE IF NOT EXISTS `channel_post_reactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `post_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `emoji` VARCHAR(10) NOT NULL COMMENT 'Emoji реакції (👍, ❤️, 🔥, etc)',
  `created_time` BIGINT UNSIGNED NOT NULL,

  UNIQUE KEY `unique_reaction` (`post_id`, `user_id`, `emoji`),
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`),
  FOREIGN KEY (`post_id`) REFERENCES `channel_posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця реакцій на коментарі
CREATE TABLE IF NOT EXISTS `channel_comment_reactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `comment_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `emoji` VARCHAR(10) NOT NULL,
  `created_time` BIGINT UNSIGNED NOT NULL,

  UNIQUE KEY `unique_reaction` (`comment_id`, `user_id`, `emoji`),
  INDEX `idx_comment_id` (`comment_id`),
  INDEX `idx_user_id` (`user_id`),
  FOREIGN KEY (`comment_id`) REFERENCES `channel_comments`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблиця переглядів постів (для статистики)
CREATE TABLE IF NOT EXISTS `channel_post_views` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `post_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'NULL для анонімних переглядів',
  `viewed_time` BIGINT UNSIGNED NOT NULL,
  `ip_address` VARCHAR(45) DEFAULT NULL,

  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_viewed_time` (`viewed_time`),
  FOREIGN KEY (`post_id`) REFERENCES `channel_posts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Тригери для автоматичного оновлення лічильників
-- ============================================

-- Тригер для оновлення subscribers_count при підписці
DELIMITER $$
CREATE TRIGGER `update_channel_subscribers_count_after_insert`
AFTER INSERT ON `channel_subscribers`
FOR EACH ROW
BEGIN
  UPDATE `channels`
  SET `subscribers_count` = `subscribers_count` + 1
  WHERE `id` = NEW.`channel_id`;
END$$
DELIMITER ;

-- Тригер для оновлення subscribers_count при відписці
DELIMITER $$
CREATE TRIGGER `update_channel_subscribers_count_after_delete`
AFTER DELETE ON `channel_subscribers`
FOR EACH ROW
BEGIN
  UPDATE `channels`
  SET `subscribers_count` = GREATEST(0, `subscribers_count` - 1)
  WHERE `id` = OLD.`channel_id`;
END$$
DELIMITER ;

-- Тригер для оновлення posts_count при створенні поста
DELIMITER $$
CREATE TRIGGER `update_channel_posts_count_after_insert`
AFTER INSERT ON `channel_posts`
FOR EACH ROW
BEGIN
  UPDATE `channels`
  SET `posts_count` = `posts_count` + 1
  WHERE `id` = NEW.`channel_id`;
END$$
DELIMITER ;

-- Тригер для оновлення posts_count при видаленні поста
DELIMITER $$
CREATE TRIGGER `update_channel_posts_count_after_delete`
AFTER DELETE ON `channel_posts`
FOR EACH ROW
BEGIN
  UPDATE `channels`
  SET `posts_count` = GREATEST(0, `posts_count` - 1)
  WHERE `id` = OLD.`channel_id`;
END$$
DELIMITER ;

-- Тригер для оновлення comments_count
DELIMITER $$
CREATE TRIGGER `update_post_comments_count_after_insert`
AFTER INSERT ON `channel_comments`
FOR EACH ROW
BEGIN
  UPDATE `channel_posts`
  SET `comments_count` = `comments_count` + 1
  WHERE `id` = NEW.`post_id`;
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER `update_post_comments_count_after_delete`
AFTER DELETE ON `channel_comments`
FOR EACH ROW
BEGIN
  UPDATE `channel_posts`
  SET `comments_count` = GREATEST(0, `comments_count` - 1)
  WHERE `id` = OLD.`post_id`;
END$$
DELIMITER ;
