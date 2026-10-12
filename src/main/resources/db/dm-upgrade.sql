-- Direct Message System Enhancement - Database Migration Script
-- Date: 2026-06-23
-- Compatible with MySQL 5.7+

USE ai_tef;

-- =====================================================
-- 1. Create thread metadata table
-- =====================================================

CREATE TABLE IF NOT EXISTS direct_message_thread_meta (
    thread_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    pinned_at DATETIME DEFAULT NULL,
    archived_at DATETIME DEFAULT NULL,
    muted_at DATETIME DEFAULT NULL,
    PRIMARY KEY (thread_id, user_id),
    CONSTRAINT fk_dm_meta_thread FOREIGN KEY (thread_id) REFERENCES direct_message_threads(id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_meta_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_dm_meta_user_pinned (user_id, is_pinned, pinned_at),
    INDEX idx_dm_meta_user_archived (user_id, is_archived, archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. Add new columns to direct_messages table
-- =====================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_not_exists$$
CREATE PROCEDURE add_column_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;

    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- Add message status field
CALL add_column_if_not_exists('direct_messages', 'message_status',
    'VARCHAR(20) NOT NULL DEFAULT "SENT" AFTER status');

-- Add read timestamp
CALL add_column_if_not_exists('direct_messages', 'read_at',
    'DATETIME DEFAULT NULL AFTER created_at');

-- Add delivered timestamp
CALL add_column_if_not_exists('direct_messages', 'delivered_at',
    'DATETIME DEFAULT NULL AFTER created_at');

-- Add quoted message reference
CALL add_column_if_not_exists('direct_messages', 'quoted_message_id',
    'BIGINT DEFAULT NULL AFTER sender_id');

-- Cleanup procedure
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- =====================================================
-- 3. Add foreign keys and indexes
-- =====================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_foreign_key_if_not_exists$$
CREATE PROCEDURE add_foreign_key_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_constraint_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    DECLARE fk_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO fk_exists
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND CONSTRAINT_NAME = p_constraint_name
      AND CONSTRAINT_TYPE = 'FOREIGN KEY';

    IF fk_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD CONSTRAINT ', p_constraint_name, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS add_index_if_not_exists$$
CREATE PROCEDURE add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index_name;

    IF idx_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD INDEX ', p_index_name, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- Add foreign key for quoted messages
CALL add_foreign_key_if_not_exists('direct_messages', 'fk_dm_quoted_message',
    'FOREIGN KEY (quoted_message_id) REFERENCES direct_messages(id) ON DELETE SET NULL');

-- Add performance indexes
CALL add_index_if_not_exists('direct_messages', 'idx_dm_status_created',
    '(thread_id, message_status, created_at)');

CALL add_index_if_not_exists('direct_messages', 'idx_dm_quoted',
    '(quoted_message_id)');

CALL add_index_if_not_exists('direct_message_threads', 'idx_dm_threads_updated',
    '(updated_at DESC)');

-- Cleanup procedures
DROP PROCEDURE IF EXISTS add_foreign_key_if_not_exists;
DROP PROCEDURE IF EXISTS add_index_if_not_exists;

-- =====================================================
-- 4. Data migration - set default status
-- =====================================================

UPDATE direct_messages
SET message_status = 'SENT'
WHERE message_status IS NULL OR message_status = '';

UPDATE direct_messages dm
JOIN direct_message_participants dmp
    ON dmp.thread_id = dm.thread_id
    AND dmp.user_id != dm.sender_id
SET dm.message_status = 'READ',
    dm.read_at = dmp.last_read_at
WHERE dmp.last_read_at IS NOT NULL
  AND dm.created_at <= dmp.last_read_at
  AND dm.message_status = 'SENT';

-- =====================================================
-- 5. Create trigger for auto-updating message status
-- =====================================================

DROP TRIGGER IF EXISTS trg_dm_mark_read_update_status;

DELIMITER $$

CREATE TRIGGER trg_dm_mark_read_update_status
AFTER UPDATE ON direct_message_participants
FOR EACH ROW
BEGIN
    IF NEW.last_read_at IS NOT NULL
       AND (OLD.last_read_at IS NULL OR NEW.last_read_at > OLD.last_read_at) THEN
        UPDATE direct_messages
        SET message_status = 'READ',
            read_at = NEW.last_read_at
        WHERE thread_id = NEW.thread_id
          AND sender_id != NEW.user_id
          AND created_at <= NEW.last_read_at
          AND message_status != 'READ';
    END IF;
END$$

DELIMITER ;

-- =====================================================
-- Completion message
-- =====================================================

SELECT 'Direct message system upgrade completed successfully' AS status,
       NOW() AS completed_at,
       VERSION() AS mysql_version;

SHOW TABLES LIKE '%direct_message%';
