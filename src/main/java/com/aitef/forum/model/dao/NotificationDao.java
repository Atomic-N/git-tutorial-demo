package com.aitef.forum.model.dao;

import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.entity.Notification;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {
    public void create(Connection conn, Long userId, Long actorId, String type, String targetType, Long targetId, String title, String body) {
        if (userId == null || userId.equals(actorId)) {
            return;
        }
        String sql = """
                INSERT INTO notifications (user_id, actor_id, type, target_type, target_id, title, body)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            if (actorId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, actorId);
            }
            ps.setString(3, type);
            ps.setString(4, targetType);
            ps.setLong(5, targetId);
            ps.setString(6, title);
            ps.setString(7, body);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create notification", e);
        }
    }

    public Page<Notification> listByUser(Long userId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(5, size));
        String listSql = """
                SELECT n.*, u.display_name AS actor_name, u.username AS actor_username, u.avatar_url AS actor_avatar_url
                FROM notifications n
                LEFT JOIN users u ON u.id = n.actor_id
                WHERE n.user_id = ?
                ORDER BY n.created_at DESC
                LIMIT ? OFFSET ?
                """;
        String countSql = "SELECT COUNT(*) FROM notifications WHERE user_id = ?";
        List<Notification> notifications = new ArrayList<>();
        long total;
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement count = conn.prepareStatement(countSql)) {
                count.setLong(1, userId);
                try (ResultSet rs = count.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(listSql)) {
                ps.setLong(1, userId);
                ps.setInt(2, safeSize);
                ps.setInt(3, (safePage - 1) * safeSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(map(rs));
                    }
                }
            }
            return new Page<>(notifications, safePage, safeSize, total);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list notifications", e);
        }
    }

    public int countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count unread notifications", e);
        }
    }

    public void markAllRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark notifications read", e);
        }
    }

    private Notification map(ResultSet rs) throws Exception {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setActorId(DaoMapper.nullableLong(rs, "actor_id"));
        notification.setActorName(rs.getString("actor_name"));
        notification.setActorUsername(rs.getString("actor_username"));
        notification.setActorAvatarUrl(rs.getString("actor_avatar_url"));
        notification.setType(rs.getString("type"));
        notification.setTargetType(rs.getString("target_type"));
        notification.setTargetId(rs.getLong("target_id"));
        notification.setTitle(rs.getString("title"));
        notification.setBody(rs.getString("body"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        return notification;
    }
}
