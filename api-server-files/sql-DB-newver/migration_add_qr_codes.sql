-- ============================================
-- WorldMates Messenger - Add QR Codes Migration
-- Version: 1.0
-- Date: 2026-01-08
-- Description: Add QR code fields to Groups
-- ============================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- ============================================
-- Add QR code to Groups
-- ============================================

-- Check if column exists before adding
SET @dbname = DATABASE();
SET @tablename = 'Wo_Groups';
SET @columnname = 'qr_code';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT ''Column already exists'' AS Status;',
  'ALTER TABLE Wo_Groups ADD COLUMN qr_code VARCHAR(50) DEFAULT NULL COMMENT ''QR code for quick join'';'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add index for QR code in Groups
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_qr_code')
  ) > 0,
  'SELECT ''Index already exists'' AS Status;',
  'CREATE INDEX idx_qr_code ON Wo_Groups(qr_code);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- Add is_muted to ChannelSubscribers (if not exists)
-- ============================================

SET @tablename = 'Wo_ChannelSubscribers';
SET @columnname = 'is_muted';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT ''Column already exists'' AS Status;',
  'ALTER TABLE Wo_ChannelSubscribers ADD COLUMN is_muted TINYINT(1) DEFAULT 0 COMMENT ''Mute notifications'';'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add index for muted channels
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (index_name = 'idx_channel_muted')
  ) > 0,
  'SELECT ''Index already exists'' AS Status;',
  'CREATE INDEX idx_channel_muted ON Wo_ChannelSubscribers(channel_id, user_id, is_muted);'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- Migration complete
-- ============================================

SELECT 'QR codes migration completed successfully!' AS Status;
SELECT
  (SELECT COUNT(*) FROM Wo_Groups WHERE qr_code IS NOT NULL) AS Groups_With_QR,
  (SELECT COUNT(*) FROM Wo_Channels WHERE qr_code IS NOT NULL) AS Channels_With_QR;
