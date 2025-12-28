-- Migration: Add Story Comments Support
-- Date: 2025-12-28
-- Description: Adds table for story comments and extends story media with duration field

-- 1. Create Story Comments table
CREATE TABLE IF NOT EXISTS `Wo_StoryComments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `story_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `text` text DEFAULT NULL,
  `time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `story_id` (`story_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Add duration field to Wo_UserStoryMedia (if not exists)
ALTER TABLE `Wo_UserStoryMedia`
ADD COLUMN IF NOT EXISTS `duration` int(11) NOT NULL DEFAULT 0 COMMENT 'Video duration in seconds';

-- 3. Add duration field to wo_userstorymedia (lowercase table - compatibility)
ALTER TABLE `wo_userstorymedia`
ADD COLUMN IF NOT EXISTS `duration` int(11) NOT NULL DEFAULT 0 COMMENT 'Video duration in seconds';

-- 4. Add indexes for better performance
ALTER TABLE `Wo_UserStory`
ADD INDEX IF NOT EXISTS `idx_user_expire` (`user_id`, `expire`),
ADD INDEX IF NOT EXISTS `idx_posted` (`posted`);

ALTER TABLE `Wo_Story_Seen`
ADD INDEX IF NOT EXISTS `idx_story_user` (`story_id`, `user_id`);

-- 5. Create lowercase version of StoryComments table for compatibility
CREATE TABLE IF NOT EXISTS `wo_storycomments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `story_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `text` text DEFAULT NULL,
  `time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `story_id` (`story_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
