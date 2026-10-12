package com.aitef.forum.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReactionDao {
    public boolean exists(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "SELECT id FROM reactions WHERE user_id = ? AND target_type = ? AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check reaction", e);
        }
    }

    public void add(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "INSERT IGNORE INTO reactions (user_id, target_type, target_id, reaction_type) VALUES (?, ?, ?, 'LIKE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add reaction", e);
        }
    }

    public void remove(Connection conn, Long userId, String targetType, Long targetId) {
        String sql = "DELETE FROM reactions WHERE user_id = ? AND target_type = ? AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove reaction", e);
        }
    }

    public int refreshCount(Connection conn, String targetType, Long targetId) {
        String table = "TOPIC".equals(targetType) ? "topics" : "posts";
        String sql = "UPDATE " + table + " SET likes_count = (SELECT COUNT(*) FROM reactions WHERE target_type = ? AND target_id = ?) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetType);
            ps.setLong(2, targetId);
            ps.setLong(3, targetId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh reaction count", e);
        }

        String countSql = "SELECT COUNT(*) FROM reactions WHERE target_type = ? AND target_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setString(1, targetType);
            ps.setLong(2, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count reactions", e);
        }
    }
}
