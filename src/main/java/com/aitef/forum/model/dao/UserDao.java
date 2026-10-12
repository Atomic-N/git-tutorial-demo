package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;
import com.aitef.forum.model.dto.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find user by id", e);
        }
    }

    public Optional<User> findById(Connection conn, Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find user by id", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find user by username", e);
        }
    }

    public Optional<User> findByUsernameOrEmail(String account) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account);
            ps.setString(2, account);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find user by account", e);
        }
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check user existence", e);
        }
    }

    public Long create(User user) {
        String sql = """
                INSERT INTO users (username, email, password_hash, display_name, avatar_url, bio, location, website, trust_level, role)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getDisplayName());
            ps.setString(5, user.getAvatarUrl());
            ps.setString(6, user.getBio());
            ps.setString(7, user.getLocation());
            ps.setString(8, user.getWebsite());
            ps.setInt(9, user.getTrustLevel());
            ps.setString(10, user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for user");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create user", e);
        }
    }

    public void updateProfile(User user) {
        String sql = """
                UPDATE users
                SET display_name = ?, bio = ?, location = ?, website = ?, avatar_url = ?
                WHERE id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getDisplayName());
            ps.setString(2, user.getBio());
            ps.setString(3, user.getLocation());
            ps.setString(4, user.getWebsite());
            ps.setString(5, user.getAvatarUrl());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update profile", e);
        }
    }

    public void incrementLikesReceived(Connection conn, Long userId, int delta) {
        if (userId == null || delta == 0) {
            return;
        }
        String sql = "UPDATE users SET likes_received_count = GREATEST(0, likes_received_count + ?) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update likes received", e);
        }
    }

    public void updateLastSeen(Long userId) {
        String sql = "UPDATE users SET last_seen_at = NOW() WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update last seen time", e);
        }
    }

    public void updateTrustLevel(Connection conn, Long userId, int trustLevel) {
        String sql = "UPDATE users SET trust_level = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trustLevel);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update trust level", e);
        }
    }

    public List<User> listForAdmin(int limit) {
        String sql = """
                SELECT * FROM users
                ORDER BY role = 'ADMIN' DESC, trust_level DESC, posts_count DESC, created_at DESC
                LIMIT ?
                """;
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
            return users;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list users for admin", e);
        }
    }

    public Page<User> pageForAdmin(int page, int size) {
        return pageForAdmin(page, size, "", "", "", null);
    }

    public Page<User> pageForAdmin(int page, int size, String keyword, String status, String role, Integer trustLevel) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(10, size));
        List<Object> params = new ArrayList<>();
        String where = buildAdminUserWhere(keyword, status, role, trustLevel, params);
        String countSql = "SELECT COUNT(*) FROM users u " + where;
        String listSql = """
                SELECT u.*
                FROM users u
                %s
                ORDER BY UPPER(u.role) = 'ADMIN' DESC,
                         UPPER(u.role) = 'MODERATOR' DESC,
                         u.status = 'SUSPENDED' DESC,
                         u.trust_level DESC,
                         u.posts_count DESC,
                         u.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(where);
        List<User> users = new ArrayList<>();
        long total;
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                bind(countPs, params);
                try (ResultSet countRs = countPs.executeQuery()) {
                    total = countRs.next() ? countRs.getLong(1) : 0L;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(listSql)) {
                int index = bind(ps, params);
                ps.setInt(index++, safeSize);
                ps.setInt(index, (safePage - 1) * safeSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        users.add(map(rs));
                    }
                }
            }
            return new Page<>(users, safePage, safeSize, total);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to page users for admin", e);
        }
    }

    private String buildAdminUserWhere(String keyword, String status, String role, Integer trustLevel, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (u.username LIKE ? OR u.display_name LIKE ? OR u.email LIKE ?) ");
            String value = "%" + keyword.trim() + "%";
            params.add(value);
            params.add(value);
            params.add(value);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND UPPER(u.status) = ? ");
            params.add(status.trim().toUpperCase());
        }
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            where.append("""
                     AND (
                        UPPER(u.role) = ?
                        OR EXISTS (
                            SELECT 1
                            FROM user_roles ur_filter
                            JOIN roles r_filter ON r_filter.id = ur_filter.role_id
                            WHERE ur_filter.user_id = u.id AND r_filter.code = ?
                        )
                     )
                    """);
            String normalizedRole = role.trim().toUpperCase();
            params.add(normalizedRole);
            params.add(normalizedRole);
        }
        if (trustLevel != null && trustLevel >= 0) {
            where.append(" AND u.trust_level = ? ");
            params.add(Math.min(4, trustLevel));
        }
        return where.toString();
    }

    private int bind(PreparedStatement ps, List<Object> params) throws Exception {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Integer value) {
                ps.setInt(index++, value);
            } else {
                ps.setString(index++, String.valueOf(param));
            }
        }
        return index;
    }

    public void updateStatusAndTrust(Connection conn, Long userId, String status, int trustLevel) {
        String sql = "UPDATE users SET status = ?, trust_level = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, trustLevel);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update user moderation", e);
        }
    }

    public void updatePrimaryRole(Connection conn, Long userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update primary role", e);
        }
    }

    public List<User> listActiveUsers(int limit) {
        String sql = """
                SELECT * FROM users
                WHERE status = 'ACTIVE'
                ORDER BY posts_count DESC, topics_count DESC, created_at DESC
                LIMIT ?
                """;
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
            return users;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list active users", e);
        }
    }

    User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setBio(rs.getString("bio"));
        user.setLocation(rs.getString("location"));
        user.setWebsite(rs.getString("website"));
        user.setTrustLevel(rs.getInt("trust_level"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        user.setTopicsCount(rs.getInt("topics_count"));
        user.setPostsCount(rs.getInt("posts_count"));
        user.setLikesReceivedCount(rs.getInt("likes_received_count"));
        user.setLastSeenAt(DaoMapper.localDateTime(rs, "last_seen_at"));
        user.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        user.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        return user;
    }

    private User map(ResultSet rs) throws Exception {
        User user = mapUser(rs);
        RoleDao roleDao = new RoleDao();
        try (Connection conn = Database.getConnection()) {
            user.setRoleCodes(roleDao.findCodesByUserId(conn, user.getId()));
            user.setModeratedCategoryIds(roleDao.findModeratedCategoryIdsByUserId(conn, user.getId()));
        }
        return user;
    }
}
