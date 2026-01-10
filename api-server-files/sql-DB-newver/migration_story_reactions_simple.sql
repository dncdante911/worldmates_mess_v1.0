-- Simple Migration for Story Reactions (MariaDB 10.11 compatible)
-- Execute this file step by step in phpMyAdmin or MySQL client

-- Step 1: Create Wo_StoryReactions table
CREATE TABLE IF NOT EXISTS `Wo_StoryReactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `story_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `reaction` varchar(30) NOT NULL DEFAULT 'like',
  `time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_story_user` (`story_id`, `user_id`),
  KEY `idx_story_id` (`story_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Add comment_count column if not exists
ALTER TABLE `Wo_UserStory`
ADD COLUMN `comment_count` int(11) NOT NULL DEFAULT 0;

-- Step 3: Add reaction_count column if not exists
ALTER TABLE `Wo_UserStory`
ADD COLUMN `reaction_count` int(11) NOT NULL DEFAULT 0;

-- Step 4: Add indexes
ALTER TABLE `Wo_UserStory`
ADD INDEX `idx_comment_count` (`comment_count`);

ALTER TABLE `Wo_UserStory`
ADD INDEX `idx_reaction_count` (`reaction_count`);

-- Success!
SELECT 'Migration completed! Wo_StoryReactions table created.' as Status;
