-- =====================================================
-- 📦 CLOUD BACKUP: Таблица настроек автозагрузки медиа
-- =====================================================
-- Дата: 2026-01-06
-- Описание: Хранит настройки автозагрузки медиа для каждого пользователя

CREATE TABLE IF NOT EXISTS `Wo_UserMediaSettings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,

  -- Настройки автозагрузки фото (wifi_only, always, never)
  `auto_download_photos` enum('wifi_only','always','never') NOT NULL DEFAULT 'wifi_only',

  -- Настройки автозагрузки видео (wifi_only, always, never)
  `auto_download_videos` enum('wifi_only','always','never') NOT NULL DEFAULT 'wifi_only',

  -- Настройки автозагрузки аудио (wifi_only, always, never)
  `auto_download_audio` enum('wifi_only','always','never') NOT NULL DEFAULT 'always',

  -- Настройки автозагрузки документов (wifi_only, always, never)
  `auto_download_documents` enum('wifi_only','always','never') NOT NULL DEFAULT 'wifi_only',

  -- Сжатие фото при загрузке (0=нет, 1=да)
  `compress_photos` tinyint(1) NOT NULL DEFAULT 1,

  -- Сжатие видео при загрузке (0=нет, 1=да)
  `compress_videos` tinyint(1) NOT NULL DEFAULT 1,

  -- Включен ли Cloud Backup (0=нет, 1=да)
  `backup_enabled` tinyint(1) NOT NULL DEFAULT 1,

  -- Последнее время резервного копирования (Unix timestamp)
  `last_backup_time` int(11) DEFAULT NULL,

  -- Дата создания настроек
  `created_at` int(11) NOT NULL,

  -- Дата последнего обновления
  `updated_at` int(11) NOT NULL,

  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `backup_enabled` (`backup_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Добавляем константу в tabels.php
-- =====================================================
-- После создания таблицы, добавьте в api-server-files/assets/includes/tabels.php:
-- define('T_USER_MEDIA_SETTINGS', 'Wo_UserMediaSettings');
