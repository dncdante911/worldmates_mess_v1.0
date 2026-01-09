-- ============================================
-- WorldMates Messenger - Channels Migration
-- Version: 1.0
-- Date: 2026-01-08
-- ============================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- ============================================
-- Table: Wo_Channels
-- Description: Main channels table
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_Channels` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT 'Channel owner/creator',
  `channel_name` varchar(255) NOT NULL,
  `channel_username` varchar(100) DEFAULT NULL COMMENT 'Unique @username',
  `channel_description` text DEFAULT NULL,
  `channel_category` varchar(100) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `cover` varchar(500) DEFAULT NULL,
  `is_private` tinyint(1) DEFAULT 0 COMMENT '0=public, 1=private',
  `is_verified` tinyint(1) DEFAULT 0,
  `subscribers_count` int(11) DEFAULT 0,
  `posts_count` int(11) DEFAULT 0,
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) DEFAULT NULL,
  `qr_code` varchar(50) DEFAULT NULL COMMENT 'QR code for quick subscribe',
  PRIMARY KEY (`id`),
  UNIQUE KEY `channel_username` (`channel_username`),
  KEY `user_id` (`user_id`),
  KEY `created_at` (`created_at`),
  KEY `subscribers_count` (`subscribers_count`),
  KEY `qr_code` (`qr_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_ChannelSubscribers
-- Description: Channel subscribers/members
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_ChannelSubscribers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channel_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `subscribed_at` int(11) NOT NULL,
  `is_muted` tinyint(1) DEFAULT 0 COMMENT 'Mute notifications',
  PRIMARY KEY (`id`),
  UNIQUE KEY `channel_user` (`channel_id`, `user_id`),
  KEY `channel_id` (`channel_id`),
  KEY `user_id` (`user_id`),
  KEY `subscribed_at` (`subscribed_at`),
  KEY `channel_muted` (`channel_id`, `user_id`, `is_muted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_ChannelPosts
-- Description: Channel posts/messages
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_ChannelPosts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channel_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL COMMENT 'Post author (admin)',
  `text` text DEFAULT NULL,
  `media` text DEFAULT NULL COMMENT 'JSON array of media URLs',
  `views_count` int(11) DEFAULT 0,
  `reactions_count` int(11) DEFAULT 0,
  `comments_count` int(11) DEFAULT 0,
  `shares_count` int(11) DEFAULT 0,
  `is_pinned` tinyint(1) DEFAULT 0,
  `disable_comments` tinyint(1) DEFAULT 0,
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) DEFAULT NULL,
  `deleted_at` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `channel_id` (`channel_id`),
  KEY `user_id` (`user_id`),
  KEY `created_at` (`created_at`),
  KEY `is_pinned` (`is_pinned`),
  KEY `channel_created` (`channel_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_ChannelComments
-- Description: Comments on channel posts
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_ChannelComments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `text` text NOT NULL,
  `reply_to_id` bigint(20) DEFAULT NULL COMMENT 'Reply to another comment',
  `reactions_count` int(11) DEFAULT 0,
  `created_at` int(11) NOT NULL,
  `updated_at` int(11) DEFAULT NULL,
  `deleted_at` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  KEY `created_at` (`created_at`),
  KEY `reply_to_id` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_ChannelAdmins
-- Description: Channel administrators/moderators
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_ChannelAdmins` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channel_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `role` enum('admin','moderator') DEFAULT 'admin',
  `added_at` int(11) NOT NULL,
  `added_by` int(11) NOT NULL COMMENT 'User who added this admin',
  PRIMARY KEY (`id`),
  UNIQUE KEY `channel_user` (`channel_id`, `user_id`),
  KEY `channel_id` (`channel_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_ChannelSettings
-- Description: Channel settings/preferences
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_ChannelSettings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `channel_id` bigint(20) NOT NULL,
  `show_author_signature` tinyint(1) DEFAULT 1,
  `allow_comments` tinyint(1) DEFAULT 1,
  `allow_reactions` tinyint(1) DEFAULT 1,
  `allow_shares` tinyint(1) DEFAULT 1,
  `moderate_comments` tinyint(1) DEFAULT 0,
  `slow_mode` int(11) DEFAULT 0 COMMENT 'Seconds between posts (0=disabled)',
  `notify_subscribers` tinyint(1) DEFAULT 1,
  `show_statistics` tinyint(1) DEFAULT 1,
  `updated_at` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: Wo_MessageViews (if not exists)
-- Description: Track views for channel posts
-- ============================================

CREATE TABLE IF NOT EXISTS `Wo_MessageViews` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message_id` bigint(20) DEFAULT NULL COMMENT 'For regular messages',
  `post_id` bigint(20) DEFAULT NULL COMMENT 'For channel posts',
  `user_id` int(11) NOT NULL,
  `viewed_at` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `message_user` (`message_id`, `user_id`),
  UNIQUE KEY `post_user` (`post_id`, `user_id`),
  KEY `message_id` (`message_id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Indexes for performance optimization
-- ============================================

-- Channel search optimization
ALTER TABLE `Wo_Channels` ADD FULLTEXT KEY `search_channel` (`channel_name`, `channel_description`);

-- Foreign key constraints (optional, for referential integrity)
-- Uncomment if you want strict foreign keys

-- ALTER TABLE `Wo_ChannelSubscribers`
--   ADD CONSTRAINT `fk_subscriber_channel` FOREIGN KEY (`channel_id`) REFERENCES `Wo_Channels` (`id`) ON DELETE CASCADE,
--   ADD CONSTRAINT `fk_subscriber_user` FOREIGN KEY (`user_id`) REFERENCES `Wo_Users` (`user_id`) ON DELETE CASCADE;

-- ALTER TABLE `Wo_ChannelPosts`
--   ADD CONSTRAINT `fk_post_channel` FOREIGN KEY (`channel_id`) REFERENCES `Wo_Channels` (`id`) ON DELETE CASCADE,
--   ADD CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `Wo_Users` (`user_id`) ON DELETE CASCADE;

-- ALTER TABLE `Wo_ChannelComments`
--   ADD CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `Wo_ChannelPosts` (`id`) ON DELETE CASCADE,
--   ADD CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `Wo_Users` (`user_id`) ON DELETE CASCADE;

-- ALTER TABLE `Wo_ChannelAdmins`
--   ADD CONSTRAINT `fk_admin_channel` FOREIGN KEY (`channel_id`) REFERENCES `Wo_Channels` (`id`) ON DELETE CASCADE,
--   ADD CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `Wo_Users` (`user_id`) ON DELETE CASCADE;

-- ALTER TABLE `Wo_ChannelSettings`
--   ADD CONSTRAINT `fk_settings_channel` FOREIGN KEY (`channel_id`) REFERENCES `Wo_Channels` (`id`) ON DELETE CASCADE;

-- ============================================
-- Insert default settings for existing channels
-- ============================================

INSERT INTO `Wo_ChannelSettings` (`channel_id`, `show_author_signature`, `allow_comments`, `allow_reactions`, `allow_shares`, `moderate_comments`, `slow_mode`, `notify_subscribers`, `show_statistics`)
SELECT `id`, 1, 1, 1, 1, 0, 0, 1, 1
FROM `Wo_Channels`
WHERE `id` NOT IN (SELECT `channel_id` FROM `Wo_ChannelSettings`);

-- ============================================
-- Migration complete
-- ============================================

-- Log migration
SELECT 'Channels migration completed successfully!' AS Status;
SELECT COUNT(*) AS Total_Channels FROM `Wo_Channels`;
SELECT COUNT(*) AS Total_Subscribers FROM `Wo_ChannelSubscribers`;
SELECT COUNT(*) AS Total_Posts FROM `Wo_ChannelPosts`;
SELECT COUNT(*) AS Total_Comments FROM `Wo_ChannelComments`;
SELECT COUNT(*) AS Total_Admins FROM `Wo_ChannelAdmins`;
