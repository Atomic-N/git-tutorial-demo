
-- 私信系统增强 - 数据库升级脚本（兼容版本）
-- 日期: 2026-06-23
-- 说明: 使用存储过程来实现 IF NOT EXISTS 逻辑

USE ai_tef;

-- =====================================================
-- 1. 创建会话元数据表
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
-- 2. 添加消息表新字段（使用存储过程处理 IF NOT EXISTS）
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

-- 添加消息状态字段
CALL add_column_if_not_exists('direct_messages', 'message_status',
    "VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT '消息状态: SENDING, SENT, DELIVERED, READ' AFTER status");

-- 添加已读时间
CALL add_column_if_not_exists('direct_messages', 'read_at',
    'DATETIME DEFAULT NULL COMMENT "消息被接收方阅读的时间" AFTER created_at');

-- 添加送达时间
CALL add_column_if_not_exists('direct_messages', 'delivered_at',
    'DATETIME DEFAULT NULL COMMENT "消息送达到接收方设备的时间" AFTER created_at');

-- 添加引用消息ID
CALL add_column_if_not_exists('direct_messages', 'quoted_message_id',
    'BIGINT DEFAULT NULL COMMENT "被引用的消息ID（用于引用回复功能）" AFTER sender_id');

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- =====================================================
-- 3. 添加外键和索引（使用存储过程处理重复）
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

-- 添加引用消息外键
CALL add_foreign_key_if_not_exists('direct_messages', 'fk_dm_quoted_message',
    'FOREIGN KEY (quoted_message_id) REFERENCES direct_messages(id) ON DELETE SET NULL');

-- 添加索引
CALL add_index_if_not_exists('direct_messages', 'idx_dm_status_created',
    '(thread_id, message_status, created_at)');

CALL add_index_if_not_exists('direct_messages', 'idx_dm_quoted',
    '(quoted_message_id)');

CALL add_index_if_not_exists('direct_message_threads', 'idx_dm_threads_updated',
    '(updated_at DESC)');

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_foreign_key_if_not_exists;
DROP PROCEDURE IF EXISTS add_index_if_not_exists;

-- =====================================================
-- 4. 数据迁移：设置默认状态
-- =====================================================

-- 将所有现有消息的 message_status 设置为 'SENT'
UPDATE direct_messages
SET message_status = 'SENT'
WHERE message_status IS NULL OR message_status = '';

-- 将已读的消息标记为 'READ'
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
-- 5. 创建触发器（自动更新消息状态）
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
-- 完成提示
-- =====================================================

SELECT '私信系统增强脚本执行完成' AS status,
       NOW() AS completed_at,
       VERSION() AS mysql_version;

-- 显示新表和字段
SHOW TABLES LIKE '%direct_message%';
DESCRIBE direct_messages;
