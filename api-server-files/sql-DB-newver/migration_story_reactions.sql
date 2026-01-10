-- Migration: Add Story Reactions Support
-- Date: 2025-01-10
-- Description: Adds dedicated table for story reactions (separate from message reactions)

-- 1. Create Story Reactions table
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

-- 2. Create lowercase version of StoryReactions table for compatibility
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Реакции на stories';

-- 3. Migrate existing story reactions from Wo_Reactions to Wo_StoryReactions
-- IMPORTANT: Only run this if you have existing story reactions in Wo_Reactions table
-- INSERT IGNORE INTO `Wo_StoryReactions` (`story_id`, `user_id`, `reaction`, `time`)
-- SELECT `story_id`, `user_id`, `reaction`, UNIX_TIMESTAMP() as `time`
-- FROM `Wo_Reactions`
-- WHERE `story_id` IS NOT NULL AND `story_id` > 0;

-- 4. Add comment_count and reaction_count to Wo_UserStory for faster queries
ALTER TABLE `Wo_UserStory`
ADD COLUMN IF NOT EXISTS `comment_count` int(11) NOT NULL DEFAULT 0 COMMENT 'Cached comment count',
ADD COLUMN IF NOT EXISTS `reaction_count` int(11) NOT NULL DEFAULT 0 COMMENT 'Cached total reaction count';

ALTER TABLE `wo_userstory`
ADD COLUMN IF NOT EXISTS `comment_count` int(11) NOT NULL DEFAULT 0 COMMENT 'Cached comment count',
ADD COLUMN IF NOT EXISTS `reaction_count` int(11) NOT NULL DEFAULT 0 COMMENT 'Cached total reaction count';

-- 5. Add indexes for better performance
ALTER TABLE `Wo_UserStory`
ADD INDEX IF NOT EXISTS `idx_comment_count` (`comment_count`),
ADD INDEX IF NOT EXISTS `idx_reaction_count` (`reaction_count`);
