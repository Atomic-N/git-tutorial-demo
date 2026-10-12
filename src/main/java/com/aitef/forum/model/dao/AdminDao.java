package com.aitef.forum.model.dao;

import com.aitef.forum.model.dto.AdminStats;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminDao {
    public AdminStats stats() {
        AdminStats stats = new AdminStats();
        try (Connection conn = Database.getConnection()) {
            stats.setUsersCount(count(conn, "SELECT COUNT(*) FROM users"));
            stats.setTopicsCount(count(conn, "SELECT COUNT(*) FROM topics WHERE status <> 'DELETED'"));
            stats.setPostsCount(count(conn, "SELECT COUNT(*) FROM posts WHERE status <> 'DELETED'"));
            stats.setPendingReportsCount(count(conn, "SELECT COUNT(*) FROM reports WHERE status = 'PENDING'"));
            stats.setChannelsCount(count(conn, "SELECT COUNT(*) FROM chat_channels"));
            stats.setMessagesCount(count(conn, "SELECT COUNT(*) FROM chat_messages WHERE status <> 'DELETED'"));
            stats.setDirectMessageThreadsCount(count(conn, "SELECT COUNT(*) FROM direct_message_threads"));
            stats.setAuditLogsCount(count(conn, "SELECT COUNT(*) FROM audit_logs"));
            stats.setSuspendedUsersCount(count(conn, "SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'"));
            stats.setAdminUsersCount(count(conn, """
                    SELECT COUNT(DISTINCT u.id)
                    FROM users u
                    LEFT JOIN user_roles ur ON ur.user_id = u.id
                    LEFT JOIN roles r ON r.id = ur.role_id
                    WHERE UPPER(u.role) = 'ADMIN' OR r.code = 'ADMIN'
                    """));
            stats.setModeratorUsersCount(count(conn, """
                    SELECT COUNT(DISTINCT u.id)
                    FROM users u
                    LEFT JOIN user_roles ur ON ur.user_id = u.id
                    LEFT JOIN roles r ON r.id = ur.role_id
                    WHERE UPPER(u.role) = 'MODERATOR' OR r.code = 'MODERATOR'
                    """));
            stats.setActiveTodayUsersCount(count(conn, "SELECT COUNT(*) FROM users WHERE last_seen_at >= CURDATE()"));
            stats.setNewTodayUsersCount(count(conn, "SELECT COUNT(*) FROM users WHERE created_at >= CURDATE()"));
            stats.setTopicsTodayCount(count(conn, "SELECT COUNT(*) FROM topics WHERE status <> 'DELETED' AND created_at >= CURDATE()"));
            stats.setPostsTodayCount(count(conn, "SELECT COUNT(*) FROM posts WHERE status <> 'DELETED' AND created_at >= CURDATE()"));
            return stats;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load admin stats", e);
        }
    }

    private long count(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
