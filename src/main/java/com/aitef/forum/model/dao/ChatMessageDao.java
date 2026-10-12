package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.ChatMessage;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDao {
    public List<ChatMessage> listByChannelId(Long channelId, int limit) {
        String sql = """
                SELECT m.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                       rp.raw_content AS reply_preview
                FROM chat_messages m
                JOIN users u ON u.id = m.user_id
                LEFT JOIN chat_messages rp ON rp.id = m.reply_to_message_id
                WHERE m.channel_id = ? AND m.status <> 'DELETED'
                ORDER BY m.created_at DESC
                LIMIT ?
                """;
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(0, map(rs));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list chat messages", e);
        }
    }

    public Long create(Connection conn, Long channelId, Long userId, Long replyToMessageId, String rawContent, String cookedContent) {
        String sql = """
                INSERT INTO chat_messages (channel_id, user_id, reply_to_message_id, raw_content, cooked_content)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, channelId);
            ps.setLong(2, userId);
            if (replyToMessageId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, replyToMessageId);
            }
            ps.setString(4, rawContent);
            ps.setString(5, cookedContent);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for chat message");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create chat message", e);
        }
    }

    public ChatMessage findById(Connection conn, Long id) {
        String sql = "SELECT id, channel_id, raw_content FROM chat_messages WHERE id = ? AND status <> 'DELETED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ChatMessage message = new ChatMessage();
                message.setId(rs.getLong("id"));
                message.setChannelId(rs.getLong("channel_id"));
                message.setRawContent(rs.getString("raw_content"));
                return message;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load chat message", e);
        }
    }

    private ChatMessage map(ResultSet rs) throws Exception {
        ChatMessage message = new ChatMessage();
        message.setId(rs.getLong("id"));
        message.setChannelId(rs.getLong("channel_id"));
        message.setUserId(rs.getLong("user_id"));
        message.setReplyToMessageId(DaoMapper.nullableLong(rs, "reply_to_message_id"));
        message.setRawContent(rs.getString("raw_content"));
        message.setCookedContent(rs.getString("cooked_content"));
        message.setStatus(rs.getString("status"));
        message.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        message.setAuthorUsername(rs.getString("author_username"));
        message.setAuthorDisplayName(rs.getString("author_display_name"));
        message.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
        message.setReplyPreview(rs.getString("reply_preview"));
        return message;
    }
}
