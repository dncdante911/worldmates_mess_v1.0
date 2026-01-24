-- Migration: Add Story Reactions Support
-- Date: 2025-01-10
-- Description: Adds dedicated table for story reactions (separate from message reactions)
-- Compatible with MariaDB 10.11

-- 1. Create Story Reactions table (main table with capital W)
CREATE TABLE IF NOT EXISTS `Wo_StoryReactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `story_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `reaction` varchar(30) NOT NULL DEFAULT 'like' COMMENT 'Reaction type: like, love, haha, wow, sad, angry',
  `time` int(11) NOT NULL DEFAULT 0 COMMENT 'Unix timestamp when reaction was added',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_story_user` (`story_id`, `user_id`),
  KEY `idx_story_id` (`story_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_reaction` (`reaction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Реакции на stories';

-- 2. Create lowercase version for compatibility (if needed by some legacy code)
CREATE TABLE IF NOT EXISTS `wo_storyreactions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `story_id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL DEFAULT 0,
  `reaction` varchar(30) NOT NULL DEFAULT 'like' COMMENT 'Reaction type: like, love, haha, wow, sad, angry',
  `time` int(11) NOT NULL DEFAULT 0 COMMENT 'Unix timestamp when reaction was added',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_story_user` (`story_id`, `user_id`),
  KEY `idx_story_id` (`story_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_reaction` (`reaction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Реакции на stories (lowercase compatibility)';

-- 3. Migrate existing story reactions from Wo_Reactions to Wo_StoryReactions (if needed)
-- IMPORTANT: Only run this if you have existing story reactions in Wo_Reactions table
-- Uncomment the line below if migration is needed:
-- INSERT IGNORE INTO `Wo_StoryReactions` (`story_id`, `user_id`, `reaction`, `time`)
-- SELECT `story_id`, `user_id`, `reaction`, UNIX_TIMESTAMP() as `time`
-- FROM `Wo_Reactions`
-- WHERE `story_id` IS NOT NULL AND `story_id` > 0;

-- 4. Add comment_count and reaction_count columns to Wo_UserStory
-- MariaDB 10.11 compatible version using prepared statements

-- Add comment_count if it doesn't exist
SET @dbname = DATABASE();
SET @tablename = 'Wo_UserStory';
SET @columnname = 'comment_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname
     AND TABLE_NAME = @tablename
     AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' int(11) NOT NULL DEFAULT 0 COMMENT ''Cached comment count''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add reaction_count if it doesn't exist
SET @columnname = 'reaction_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @dbname
     AND TABLE_NAME = @tablename
     AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' int(11) NOT NULL DEFAULT 0 COMMENT ''Cached total reaction count''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 5. Add indexes for better performance (if they don't exist)
SET @indexname = 'idx_comment_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname
     AND TABLE_NAME = @tablename
     AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (comment_count)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @indexname = 'idx_reaction_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @dbname
     AND TABLE_NAME = @tablename
     AND INDEX_NAME = @indexname) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (reaction_count)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 6. Success message
SELECT 'Migration completed successfully! Wo_StoryReactions table created.' as Status;
