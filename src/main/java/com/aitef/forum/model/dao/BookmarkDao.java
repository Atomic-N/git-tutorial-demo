package com.aitef.forum.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookmarkDao {
    public boolean exists(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "SELECT id FROM bookmarks WHERE user_id = ? AND target_type = ? AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check bookmark", e);
        }
    }

    public void add(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "INSERT IGNORE INTO bookmarks (user_id, target_type, target_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add bookmark", e);
        }
    }

    public void remove(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "DELETE FROM bookmarks WHERE user_id = ? AND target_type = ? AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove bookmark", e);
        }
    }

    public int refreshTopicCount(Connection conn, Long topicId) {
        String updateSql = "UPDATE topics SET bookmarks_count = (SELECT COUNT(*) FROM bookmarks WHERE target_type = 'TOPIC' AND target_id = ?) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setLong(1, topicId);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh bookmark count", e);
        }

        String countSql = "SELECT COUNT(*) FROM bookmarks WHERE target_type = 'TOPIC' AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count bookmarks", e);
        }
    }
}
