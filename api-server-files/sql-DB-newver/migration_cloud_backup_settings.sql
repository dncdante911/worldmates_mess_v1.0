-- 📦 CLOUD BACKUP v2: Расширенные настройки облачного бэкапа
-- Дата создания: 2026-01-06

CREATE TABLE IF NOT EXISTS `Wo_UserCloudBackupSettings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,

  -- АВТОЗАГРУЗКА МЕДИА (Мобильный интернет)
  `mobile_photos` tinyint(1) NOT NULL DEFAULT 0,
  `mobile_videos` tinyint(1) NOT NULL DEFAULT 0,
  `mobile_files` tinyint(1) NOT NULL DEFAULT 0,
  `mobile_videos_limit` int(11) NOT NULL DEFAULT 10,  -- МБ
  `mobile_files_limit` int(11) NOT NULL DEFAULT 10,   -- МБ

  -- АВТОЗАГРУЗКА МЕДИА (Wi-Fi)
  `wifi_photos` tinyint(1) NOT NULL DEFAULT 1,
  `wifi_videos` tinyint(1) NOT NULL DEFAULT 1,
  `wifi_files` tinyint(1) NOT NULL DEFAULT 1,
  `wifi_videos_limit` int(11) NOT NULL DEFAULT 100,   -- МБ
  `wifi_files_limit` int(11) NOT NULL DEFAULT 100,    -- МБ

  -- АВТОЗАГРУЗКА МЕДИА (Роуминг)
  `roaming_photos` tinyint(1) NOT NULL DEFAULT 0,

  -- СОХРАНЯТЬ В ГАЛЕРЕЕ
  `save_to_gallery_private_chats` tinyint(1) NOT NULL DEFAULT 0,
  `save_to_gallery_groups` tinyint(1) NOT NULL DEFAULT 0,
  `save_to_gallery_channels` tinyint(1) NOT NULL DEFAULT 0,

  -- СТРИМИНГ
  `streaming_enabled` tinyint(1) NOT NULL DEFAULT 1,

  -- УПРАВЛЕНИЕ КЭШЕМ
  `cache_size_limit` bigint(20) NOT NULL DEFAULT 3221225472,  -- 3 ГБ в байтах

  -- ОБЛАЧНЫЙ БЭКАП
  `backup_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `backup_provider` varchar(50) NOT NULL DEFAULT 'local_server',  -- local_server, google_drive, mega, dropbox
  `backup_frequency` varchar(50) NOT NULL DEFAULT 'daily',  -- never, daily, weekly, monthly
  `last_backup_time` bigint(20) DEFAULT NULL,

  -- ПРОКСИ
  `proxy_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `proxy_host` varchar(255) DEFAULT NULL,
  `proxy_port` int(11) DEFAULT NULL,

  -- Метаданные
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_backup_provider` (`backup_provider`),
  KEY `idx_backup_enabled` (`backup_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Индексы для быстрого поиска
ALTER TABLE `Wo_UserCloudBackupSettings`
  ADD CONSTRAINT `fk_cloud_backup_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Wo_Users` (`user_id`)
  ON DELETE CASCADE;
