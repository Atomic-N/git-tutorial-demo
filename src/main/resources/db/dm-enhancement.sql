-- 私信系统增强 - 数据库升级脚本
-- 日期: 2026-06-23
-- 描述: 添加置顶、归档、免打扰、消息状态、引用回复等功能

-- =====================================================
-- 1. 扩展会话元数据（置顶、归档、免打扰）
-- =====================================================

-- 会话元数据表（每个用户对每个会话的个性化设置）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='私信会话元数据：用户级别的置顶、归档、免打扰设置';


-- =====================================================
-- 2. 扩展消息表（状态、已读时间、引用回复）
-- =====================================================

-- 添加消息状态字段（如果不存在）
ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS message_status VARCHAR(20) NOT NULL DEFAULT 'SENT'
COMMENT '消息状态: SENDING, SENT, DELIVERED, READ'
AFTER status;

-- 添加已读时间
ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS read_at DATETIME DEFAULT NULL
COMMENT '消息被接收方阅读的时间'
AFTER created_at;

-- 添加送达时间
ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS delivered_at DATETIME DEFAULT NULL
COMMENT '消息送达到接收方设备的时间'
AFTER created_at;

-- 添加引用消息ID
ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS quoted_message_id BIGINT DEFAULT NULL
COMMENT '被引用的消息ID（用于引用回复功能）'
AFTER sender_id;

-- 添加引用消息外键约束
ALTER TABLE direct_messages
ADD CONSTRAINT fk_dm_quoted_message
FOREIGN KEY (quoted_message_id) REFERENCES direct_messages(id) ON DELETE SET NULL;

-- 添加索引优化查询
ALTER TABLE direct_messages
ADD INDEX IF NOT EXISTS idx_dm_status_created (thread_id, message_status, created_at);

ALTER TABLE direct_messages
ADD INDEX IF NOT EXISTS idx_dm_quoted (quoted_message_id);


-- =====================================================
-- 3. 消息附件表（支持文件、图片上传）
-- =====================================================

CREATE TABLE IF NOT EXISTS direct_message_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL COMMENT 'MIME类型',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    thumbnail_path VARCHAR(500) DEFAULT NULL COMMENT '缩略图路径（仅图片）',
    width INT DEFAULT NULL COMMENT '图片宽度（仅图片）',
    height INT DEFAULT NULL COMMENT '图片高度（仅图片）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dm_attachments_message FOREIGN KEY (message_id) REFERENCES direct_messages(id) ON DELETE CASCADE,
    INDEX idx_dm_attachments_message (message_id),
    INDEX idx_dm_attachments_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='私信附件表：支持文件、图片上传';


-- =====================================================
-- 4. 性能优化索引
-- =====================================================

-- 优化会话列表查询（按更新时间倒序）
ALTER TABLE direct_message_threads
ADD INDEX IF NOT EXISTS idx_dm_threads_updated (updated_at DESC);

-- 优化参与者查询（用户的所有会话）
ALTER TABLE direct_message_participants
ADD INDEX IF NOT EXISTS idx_dm_participants_user_read (user_id, last_read_at);


-- =====================================================
-- 5. 数据迁移：为现有消息设置默认状态
-- =====================================================

-- 将所有现有消息的 message_status 设置为 'SENT'
UPDATE direct_messages
SET message_status = 'SENT'
WHERE message_status IS NULL OR message_status = '';

-- 将已读的消息标记为 'READ'（根据 last_read_at 判断）
UPDATE direct_messages dm
JOIN direct_message_participants dmp
    ON dmp.thread_id = dm.thread_id
    AND dmp.user_id != dm.sender_id
SET dm.message_status = 'READ',
    dm.read_at = dmp.last_read_at
WHERE dmp.last_read_at IS NOT NULL
  AND dm.created_at <= dmp.last_read_at;


-- =====================================================
-- 6. 视图：方便查询用户的活跃会话
-- =====================================================

CREATE OR REPLACE VIEW v_active_dm_threads AS
SELECT
    t.id AS thread_id,
    t.subject,
    t.created_at,
    t.updated_at,
    p.user_id,
    COALESCE(m.is_pinned, FALSE) AS is_pinned,
    COALESCE(m.is_archived, FALSE) AS is_archived,
    COALESCE(m.is_muted, FALSE) AS is_muted,
    (
        SELECT COUNT(*)
        FROM direct_messages dm
        WHERE dm.thread_id = t.id
          AND dm.status <> 'DELETED'
          AND dm.sender_id != p.user_id
          AND (p.last_read_at IS NULL OR dm.created_at > p.last_read_at)
    ) AS unread_count
FROM direct_message_threads t
JOIN direct_message_participants p ON p.thread_id = t.id
LEFT JOIN direct_message_thread_meta m ON m.thread_id = t.id AND m.user_id = p.user_id
WHERE COALESCE(m.is_archived, FALSE) = FALSE;


-- =====================================================
-- 7. 触发器：自动更新消息状态
-- =====================================================

DELIMITER $$

-- 当用户标记会话已读时，自动更新消息状态
CREATE TRIGGER IF NOT EXISTS trg_dm_mark_read_update_status
AFTER UPDATE ON direct_message_participants
FOR EACH ROW
BEGIN
    IF NEW.last_read_at IS NOT NULL AND (OLD.last_read_at IS NULL OR NEW.last_read_at > OLD.last_read_at) THEN
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
-- 8. 存储过程：批量操作
-- =====================================================

DELIMITER $$

-- 批量归档会话
CREATE PROCEDURE IF NOT EXISTS sp_dm_archive_threads(
    IN p_user_id BIGINT,
    IN p_thread_ids TEXT  -- 逗号分隔的thread_id列表
)
BEGIN
    DECLARE thread_id_value BIGINT;
    DECLARE pos INT DEFAULT 1;
    DECLARE str_length INT;
    DECLARE comma_pos INT;

    SET str_length = CHAR_LENGTH(p_thread_ids);

    WHILE pos <= str_length DO
        SET comma_pos = LOCATE(',', p_thread_ids, pos);

        IF comma_pos = 0 THEN
            SET comma_pos = str_length + 1;
        END IF;

        SET thread_id_value = CAST(SUBSTRING(p_thread_ids, pos, comma_pos - pos) AS UNSIGNED);

        -- 插入或更新元数据
        INSERT INTO direct_message_thread_meta (thread_id, user_id, is_archived, archived_at)
        VALUES (thread_id_value, p_user_id, TRUE, NOW())
        ON DUPLICATE KEY UPDATE
            is_archived = TRUE,
            archived_at = NOW();

        SET pos = comma_pos + 1;
    END WHILE;
END$$

-- 批量删除会话（软删除：标记为已归档）
CREATE PROCEDURE IF NOT EXISTS sp_dm_delete_threads(
    IN p_user_id BIGINT,
    IN p_thread_ids TEXT
)
BEGIN
    CALL sp_dm_archive_threads(p_user_id, p_thread_ids);
END$$

DELIMITER ;


-- =====================================================
-- 9. 数据验证与一致性检查
-- =====================================================

-- 确保每个会话至少有2个参与者
SELECT
    thread_id,
    COUNT(*) AS participant_count
FROM direct_message_participants
GROUP BY thread_id
HAVING participant_count < 2;

-- 检查是否有孤立的消息（thread不存在）
SELECT COUNT(*) AS orphaned_messages
FROM direct_messages dm
LEFT JOIN direct_message_threads t ON t.id = dm.thread_id
WHERE t.id IS NULL;


-- =====================================================
-- 完成提示
-- =====================================================

SELECT
    '私信系统增强脚本执行完成' AS status,
    NOW() AS completed_at,
    VERSION() AS mysql_version;
