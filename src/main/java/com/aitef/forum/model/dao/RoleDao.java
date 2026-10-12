package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Role;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RoleDao {
    public List<Role> findAll() {
        String sql = "SELECT * FROM roles ORDER BY id ASC";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(map(rs));
            }
            return roles;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load roles", e);
        }
    }

    public Set<String> findCodesByUserId(Connection conn, Long userId) {
        String sql = """
                SELECT r.code
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                ORDER BY r.id ASC
                """;
        Set<String> codes = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString("code"));
                }
            }
            return codes;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load role codes", e);
        }
    }

    public Set<Long> findModeratedCategoryIdsByUserId(Connection conn, Long userId) {
        if (userId == null) {
            return Set.of();
        }
        try {
            return loadModeratedCategoryIds(conn, userId);
        } catch (Exception e) {
            if (!isMissingModeratorScopeTable(e)) {
                throw new IllegalStateException("Failed to load moderated categories", e);
            }
            try {
                ensureModeratorScopeTable(conn);
                return loadModeratedCategoryIds(conn, userId);
            } catch (Exception ignored) {
                return Set.of();
            }
        }
    }

    public void ensureModeratorScopeTable() {
        try (Connection conn = Database.getConnection()) {
            ensureModeratorScopeTable(conn);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare moderated category scope table", e);
        }
    }

    public void ensureModeratorScopeTable(Connection conn) {
        if (moderatorScopeTableExists(conn)) {
            return;
        }
        String sql = """
                CREATE TABLE IF NOT EXISTS category_moderators (
                  category_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (category_id, user_id),
                  CONSTRAINT fk_category_moderators_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
                  CONSTRAINT fk_category_moderators_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                  INDEX idx_category_moderators_user (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create moderated category scope table", e);
        }
    }

    private boolean moderatorScopeTableExists(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM category_moderators LIMIT 1")) {
            ps.executeQuery();
            return true;
        } catch (Exception e) {
            if (isMissingModeratorScopeTable(e)) {
                return false;
            }
            throw new IllegalStateException("Failed to inspect moderated category scope table", e);
        }
    }

    private Set<Long> loadModeratedCategoryIds(Connection conn, Long userId) throws Exception {
        String sql = """
                SELECT category_id
                FROM category_moderators
                WHERE user_id = ?
                ORDER BY category_id ASC
                """;
        Set<Long> categoryIds = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categoryIds.add(rs.getLong("category_id"));
                }
            }
            return categoryIds;
        }
    }

    public void replaceUserRoles(Connection conn, Long userId, List<String> codes) {
        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
            delete.setLong(1, userId);
            delete.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clear user roles", e);
        }
        if (codes == null || codes.isEmpty()) {
            return;
        }
        String sql = """
                INSERT IGNORE INTO user_roles (user_id, role_id)
                SELECT ?, id FROM roles WHERE code = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String code : codes) {
                ps.setLong(1, userId);
                ps.setString(2, code);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replace user roles", e);
        }
    }

    public void replaceModeratedCategories(Connection conn, Long userId, List<Long> categoryIds) {
        ensureModeratorScopeTable(conn);
        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM category_moderators WHERE user_id = ?")) {
            delete.setLong(1, userId);
            delete.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clear moderated categories", e);
        }
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT IGNORE INTO category_moderators (category_id, user_id)
                SELECT id, ? FROM categories WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Long categoryId : categoryIds) {
                if (categoryId == null) {
                    continue;
                }
                ps.setLong(1, userId);
                ps.setLong(2, categoryId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replace moderated categories", e);
        }
    }

    private boolean isMissingModeratorScopeTable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException && "42S02".equals(sqlException.getSQLState())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null
                    && message.contains("category_moderators")
                    && (message.contains("doesn't exist") || message.contains("does not exist") || message.contains("不存在"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Role map(ResultSet rs) throws Exception {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setCode(rs.getString("code"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        role.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        return role;
    }
}
