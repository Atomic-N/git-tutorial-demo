package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.DirectMessage;
import com.aitef.forum.model.entity.DirectMessageThread;
import com.aitef.forum.model.entity.DirectMessageThreadMeta;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DirectMessageDao {
    public Long createThread(Connection conn, String subject) {
        String sql = "INSERT INTO direct_message_threads (subject) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, subject);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new IllegalStateException("No key returned for direct message thread");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create direct message thread", e);
        }
    }

    public void addParticipant(Connection conn, Long threadId, Long userId) {
        String sql = "INSERT IGNORE INTO direct_message_participants (thread_id, user_id, last_read_at) VALUES (?, ?, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add direct message participant", e);
        }
    }

    public Optional<Long> findThreadBetween(Connection conn, Long userA, Long userB) {
        String sql = """
                SELECT p1.thread_id
                FROM direct_message_participants p1
                JOIN direct_message_participants p2 ON p2.thread_id = p1.thread_id
                WHERE p1.user_id = ? AND p2.user_id = ?
                ORDER BY p1.thread_id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userA);
            ps.setLong(2, userB);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("thread_id")) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find direct message thread", e);
        }
    }

    public Long createMessage(Connection conn, Long threadId, Long senderId, String raw, String cooked) {
        return createMessage(conn, threadId, senderId, raw, cooked, null);
    }

    public Long createMessage(Connection conn, Long threadId, Long senderId, String raw, String cooked, Long quotedMessageId) {
        String sql = """
                INSERT INTO direct_messages (thread_id, sender_id, body_raw, body_cooked, quoted_message_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, threadId);
            ps.setLong(2, senderId);
            ps.setString(3, raw);
            ps.setString(4, cooked);
            if (quotedMessageId != null) {
                ps.setLong(5, quotedMessageId);
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new IllegalStateException("No key returned for direct message");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create direct message", e);
        }
    }

    public void touchThread(Connection conn, Long threadId) {
        String sql = "UPDATE direct_message_threads SET updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to touch direct message thread", e);
        }
    }

    public void markRead(Connection conn, Long threadId, Long userId) {
        String sql = "UPDATE direct_message_participants SET last_read_at = NOW() WHERE thread_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark direct message thread read", e);
        }
    }

    public List<DirectMessageThread> listThreads(Long userId) {
        String sql = """
                SELECT t.*, partner.user_id AS partner_id, u.username AS partner_username, u.display_name AS partner_display_name, u.avatar_url AS partner_avatar_url,
                       last_msg.body_raw AS latest_preview, last_msg.created_at AS latest_message_at,
                       CASE
                         WHEN self.last_read_at IS NULL THEN TRUE
                         WHEN last_msg.created_at IS NULL THEN FALSE
                         WHEN self.last_read_at < last_msg.created_at AND last_msg.sender_id <> ? THEN TRUE
                         ELSE FALSE
                       END AS unread,
                       (SELECT COUNT(*) FROM direct_messages dm
                        WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
                        AND dm.sender_id <> ?
                        AND (self.last_read_at IS NULL OR dm.created_at > self.last_read_at)
                       ) AS unread_count
                FROM direct_message_participants self
                JOIN direct_message_threads t ON t.id = self.thread_id
                JOIN direct_message_participants partner ON partner.thread_id = t.id AND partner.user_id <> self.user_id
                JOIN users u ON u.id = partner.user_id
                LEFT JOIN direct_messages last_msg ON last_msg.id = (
                    SELECT dm.id FROM direct_messages dm
                    WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
                    ORDER BY dm.created_at DESC
                    LIMIT 1
                )
                WHERE self.user_id = ?
                ORDER BY COALESCE(last_msg.created_at, t.updated_at) DESC
                """;
        List<DirectMessageThread> threads = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    threads.add(mapThread(rs));
                }
            }
            return threads;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list direct message threads", e);
        }
    }

    public DirectMessageThread findThreadById(Long threadId, Long userId) {
        String sql = """
                SELECT t.*, partner.user_id AS partner_id, u.username AS partner_username, u.display_name AS partner_display_name, u.avatar_url AS partner_avatar_url,
                       last_msg.body_raw AS latest_preview, last_msg.created_at AS latest_message_at,
                       CASE
                         WHEN self.last_read_at IS NULL THEN TRUE
                         WHEN last_msg.created_at IS NULL THEN FALSE
                         WHEN self.last_read_at < last_msg.created_at AND last_msg.sender_id <> ? THEN TRUE
                         ELSE FALSE
                       END AS unread,
                       (SELECT COUNT(*) FROM direct_messages dm
                        WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
                        AND dm.sender_id <> ?
                        AND (self.last_read_at IS NULL OR dm.created_at > self.last_read_at)
                       ) AS unread_count
                FROM direct_message_participants self
                JOIN direct_message_threads t ON t.id = self.thread_id
                JOIN direct_message_participants partner ON partner.thread_id = t.id AND partner.user_id <> self.user_id
                JOIN users u ON u.id = partner.user_id
                LEFT JOIN direct_messages last_msg ON last_msg.id = (
                    SELECT dm.id FROM direct_messages dm
                    WHERE dm.thread_id = t.id AND dm.status <> 'DELETED'
                    ORDER BY dm.created_at DESC
                    LIMIT 1
                )
                WHERE self.thread_id = ? AND self.user_id = ?
                LIMIT 1
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, threadId);
            ps.setLong(4, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapThread(rs) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load direct message thread", e);
        }
    }

    public List<DirectMessage> listMessages(Long threadId) {
        String sql = """
                SELECT m.*,
                       u.username AS sender_username,
                       u.display_name AS sender_display_name,
                       u.avatar_url AS sender_avatar_url,
                       qm.body_raw AS quoted_message_body,
                       qu.display_name AS quoted_sender_name
                FROM direct_messages m
                JOIN users u ON u.id = m.sender_id
                LEFT JOIN direct_messages qm ON qm.id = m.quoted_message_id
                LEFT JOIN users qu ON qu.id = qm.sender_id
                WHERE m.thread_id = ? AND m.status <> 'DELETED'
                ORDER BY m.created_at ASC
                """;
        List<DirectMessage> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list direct messages", e);
        }
    }

    public List<Long> participantIds(Connection conn, Long threadId) {
        String sql = "SELECT user_id FROM direct_message_participants WHERE thread_id = ?";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("user_id"));
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load participant ids", e);
        }
    }

    public int countUnreadThreads(Long userId) {
        String sql = """
                SELECT COUNT(*)
                FROM direct_message_participants self
                JOIN direct_messages last_msg ON last_msg.id = (
                    SELECT dm.id FROM direct_messages dm
                    WHERE dm.thread_id = self.thread_id AND dm.status <> 'DELETED'
                    ORDER BY dm.created_at DESC
                    LIMIT 1
                )
                WHERE self.user_id = ? AND (self.last_read_at IS NULL OR self.last_read_at < last_msg.created_at) AND last_msg.sender_id <> ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count unread direct messages", e);
        }
    }

    private DirectMessageThread mapThread(ResultSet rs) throws Exception {
        DirectMessageThread thread = new DirectMessageThread();
        thread.setId(rs.getLong("id"));
        thread.setSubject(rs.getString("subject"));
        thread.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        thread.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        thread.setPartnerId(rs.getLong("partner_id"));
        thread.setPartnerUsername(rs.getString("partner_username"));
        thread.setPartnerDisplayName(rs.getString("partner_display_name"));
        thread.setPartnerAvatarUrl(rs.getString("partner_avatar_url"));
        thread.setLatestPreview(rs.getString("latest_preview"));
        thread.setLatestMessageAt(DaoMapper.localDateTime(rs, "latest_message_at"));
        thread.setUnread(rs.getBoolean("unread"));
        thread.setUnreadCount(rs.getInt("unread_count"));
        return thread;
    }

    private DirectMessage mapMessage(ResultSet rs) throws Exception {
        DirectMessage message = new DirectMessage();
        message.setId(rs.getLong("id"));
        message.setThreadId(rs.getLong("thread_id"));
        message.setSenderId(rs.getLong("sender_id"));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setSenderDisplayName(rs.getString("sender_display_name"));
        message.setSenderAvatarUrl(rs.getString("sender_avatar_url"));
        message.setBodyRaw(rs.getString("body_raw"));
        message.setBodyCooked(rs.getString("body_cooked"));
        message.setStatus(rs.getString("status"));
        message.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));

        // 引用消息
        try {
            Long quotedId = rs.getLong("quoted_message_id");
            if (!rs.wasNull()) {
                message.setQuotedMessageId(quotedId);
                message.setQuotedMessageBody(rs.getString("quoted_message_body"));
                message.setQuotedSenderName(rs.getString("quoted_sender_name"));
            }
        } catch (Exception e) {
            // 兼容旧数据
        }

        // 新增状态字段
        try {
            message.setMessageStatus(rs.getString("message_status"));
            message.setReadAt(DaoMapper.localDateTime(rs, "read_at"));
            message.setDeliveredAt(DaoMapper.localDateTime(rs, "delivered_at"));
        } catch (Exception e) {
            // 兼容旧数据，如果字段不存在则忽略
            message.setMessageStatus("SENT");
        }

        return message;
    }

    // ==================== 会话管理功能 ====================

    /**
     * 置顶会话
     */
    public void pinThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_pinned, pinned_at)
                VALUES (?, ?, TRUE, NOW())
                ON DUPLICATE KEY UPDATE is_pinned = TRUE, pinned_at = NOW()
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to pin thread", e);
        }
    }

    /**
     * 取消置顶会话
     */
    public void unpinThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_pinned, pinned_at)
                VALUES (?, ?, FALSE, NULL)
                ON DUPLICATE KEY UPDATE is_pinned = FALSE, pinned_at = NULL
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unpin thread", e);
        }
    }

    /**
     * 归档会话
     */
    public void archiveThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_archived, archived_at)
                VALUES (?, ?, TRUE, NOW())
                ON DUPLICATE KEY UPDATE is_archived = TRUE, archived_at = NOW()
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive thread", e);
        }
    }

    /**
     * 取消归档会话
     */
    public void unarchiveThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_archived, archived_at)
                VALUES (?, ?, FALSE, NULL)
                ON DUPLICATE KEY UPDATE is_archived = FALSE, archived_at = NULL
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unarchive thread", e);
        }
    }

    /**
     * 设置会话免打扰
     */
    public void muteThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_muted, muted_at)
                VALUES (?, ?, TRUE, NOW())
                ON DUPLICATE KEY UPDATE is_muted = TRUE, muted_at = NOW()
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mute thread", e);
        }
    }

    /**
     * 取消会话免打扰
     */
    public void unmuteThread(Connection conn, Long threadId, Long userId) {
        String sql = """
                INSERT INTO direct_message_thread_meta (thread_id, user_id, is_muted, muted_at)
                VALUES (?, ?, FALSE, NULL)
                ON DUPLICATE KEY UPDATE is_muted = FALSE, muted_at = NULL
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unmute thread", e);
        }
    }

    /**
     * 删除会话（实际上是归档）
     */
    public void deleteThread(Connection conn, Long threadId, Long userId) {
        archiveThread(conn, threadId, userId);
    }

    /**
     * 获取会话元数据
     */
    public Optional<DirectMessageThreadMeta> getThreadMeta(Long threadId, Long userId) {
        String sql = "SELECT * FROM direct_message_thread_meta WHERE thread_id = ? AND user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapThreadMeta(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get thread meta", e);
        }
    }

    private DirectMessageThreadMeta mapThreadMeta(ResultSet rs) throws Exception {
        DirectMessageThreadMeta meta = new DirectMessageThreadMeta();
        meta.setThreadId(rs.getLong("thread_id"));
        meta.setUserId(rs.getLong("user_id"));
        meta.setPinned(rs.getBoolean("is_pinned"));
        meta.setArchived(rs.getBoolean("is_archived"));
        meta.setMuted(rs.getBoolean("is_muted"));
        meta.setPinnedAt(DaoMapper.localDateTime(rs, "pinned_at"));
        meta.setArchivedAt(DaoMapper.localDateTime(rs, "archived_at"));
        meta.setMutedAt(DaoMapper.localDateTime(rs, "muted_at"));
        return meta;
    }
}
