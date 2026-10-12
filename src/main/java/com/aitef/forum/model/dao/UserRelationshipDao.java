package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserRelationshipDao {
    public void add(Long userId, Long targetUserId, String relationshipType) {
        String sql = """
                INSERT IGNORE INTO user_relationships (user_id, target_user_id, relationship_type)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, targetUserId);
            ps.setString(3, relationshipType);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add relationship", e);
        }
    }

    public void remove(Long userId, Long targetUserId, String relationshipType) {
        String sql = "DELETE FROM user_relationships WHERE user_id = ? AND target_user_id = ? AND relationship_type = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, targetUserId);
            ps.setString(3, relationshipType);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to remove relationship", e);
        }
    }

    public boolean exists(Long userId, Long targetUserId, String relationshipType) {
        String sql = "SELECT 1 FROM user_relationships WHERE user_id = ? AND target_user_id = ? AND relationship_type = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, targetUserId);
            ps.setString(3, relationshipType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check relationship", e);
        }
    }

    public List<User> listTargets(Long userId, String relationshipType) {
        String sql = """
                SELECT u.*
                FROM user_relationships ur
                JOIN users u ON u.id = ur.target_user_id
                WHERE ur.user_id = ? AND ur.relationship_type = ?
                ORDER BY ur.created_at DESC
                """;
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, relationshipType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new UserDao().mapUser(rs));
                }
            }
            return users;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list relationship targets", e);
        }
    }

    public Set<Long> findTargetIds(Long userId, String relationshipType) {
        String sql = "SELECT target_user_id FROM user_relationships WHERE user_id = ? AND relationship_type = ?";
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, relationshipType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("target_user_id"));
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list relationship ids", e);
        }
    }

    public Optional<String> findFirstBlockingType(Long userId, Long targetUserId) {
        String sql = """
                SELECT relationship_type
                FROM user_relationships
                WHERE user_id = ? AND target_user_id = ? AND relationship_type IN ('BLOCK', 'MUTE')
                ORDER BY relationship_type DESC
                LIMIT 1
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, targetUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("relationship_type")) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load blocking relationship", e);
        }
    }
}
