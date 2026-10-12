package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Badge;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BadgeDao {
    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean schemaReady;

    public List<Badge> listAll() {
        ensureSchema();
        String sql = """
                SELECT b.*, NULL AS awarded_at,
                       (SELECT COUNT(*) FROM user_badges ub WHERE ub.badge_id = b.id) AS awarded_count
                FROM badges b
                ORDER BY b.sort_order ASC, b.id ASC
                """;
        List<Badge> badges = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                badges.add(map(rs));
            }
            return badges;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load badges", e);
        }
    }

    public List<Badge> listCatalogForUser(Long userId) {
        ensureSchema();
        if (userId == null) {
            return listAll();
        }
        String sql = """
                SELECT b.*, ub.awarded_at,
                       (SELECT COUNT(*) FROM user_badges ub_count WHERE ub_count.badge_id = b.id) AS awarded_count
                FROM badges b
                LEFT JOIN user_badges ub ON ub.badge_id = b.id AND ub.user_id = ?
                ORDER BY b.sort_order ASC, b.id ASC
                """;
        List<Badge> badges = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    badges.add(map(rs));
                }
            }
            return badges;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load badge catalog", e);
        }
    }

    public List<Badge> listByUserId(Long userId) {
        ensureSchema();
        String sql = """
                SELECT b.*, ub.awarded_at,
                       (SELECT COUNT(*) FROM user_badges ub_count WHERE ub_count.badge_id = b.id) AS awarded_count
                FROM user_badges ub
                JOIN badges b ON b.id = ub.badge_id
                WHERE ub.user_id = ?
                ORDER BY b.sort_order ASC, ub.awarded_at DESC
                """;
        List<Badge> badges = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    badges.add(map(rs));
                }
            }
            return badges;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load user badges", e);
        }
    }

    public Optional<Badge> findById(Long id) {
        ensureSchema();
        if (id == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT b.*, NULL AS awarded_at,
                       (SELECT COUNT(*) FROM user_badges ub WHERE ub.badge_id = b.id) AS awarded_count
                FROM badges b
                WHERE b.id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find badge", e);
        }
    }

    public boolean existsByCodeExceptId(String code, Long ignoredId) {
        ensureSchema();
        String sql = "SELECT COUNT(*) FROM badges WHERE code = ? AND (? IS NULL OR id <> ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            if (ignoredId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, ignoredId);
                ps.setLong(3, ignoredId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check badge code", e);
        }
    }

    public Long create(Connection conn, Badge badge) {
        ensureSchema();
        String sql = """
                INSERT INTO badges (code, name, description, icon, color, sort_order, rule_type, rule_threshold)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindBadge(ps, badge);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for badge");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create badge", e);
        }
    }

    public void update(Connection conn, Badge badge) {
        ensureSchema();
        String sql = """
                UPDATE badges
                SET code = ?, name = ?, description = ?, icon = ?, color = ?, sort_order = ?,
                    rule_type = ?, rule_threshold = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = bindBadge(ps, badge);
            ps.setLong(index, badge.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update badge", e);
        }
    }

    public void delete(Connection conn, Long badgeId) {
        ensureSchema();
        try (PreparedStatement deleteAwards = conn.prepareStatement("DELETE FROM user_badges WHERE badge_id = ?");
             PreparedStatement deleteBadge = conn.prepareStatement("DELETE FROM badges WHERE id = ?")) {
            deleteAwards.setLong(1, badgeId);
            deleteAwards.executeUpdate();
            deleteBadge.setLong(1, badgeId);
            deleteBadge.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete badge", e);
        }
    }

    public void awardToUser(Connection conn, Long userId, Long badgeId) {
        ensureSchema();
        String sql = "INSERT IGNORE INTO user_badges (user_id, badge_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, badgeId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to award badge to user", e);
        }
    }

    public void revokeFromUser(Connection conn, Long userId, Long badgeId) {
        ensureSchema();
        String sql = "DELETE FROM user_badges WHERE user_id = ? AND badge_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, badgeId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to revoke badge from user", e);
        }
    }

    public List<Long> listUserIds() {
        String sql = "SELECT id FROM users WHERE status <> 'DELETED' ORDER BY id";
        List<Long> userIds = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                userIds.add(rs.getLong("id"));
            }
            return userIds;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load users for badges", e);
        }
    }

    public void awardByCode(Connection conn, Long userId, String badgeCode) {
        ensureSchema();
        String sql = """
                INSERT IGNORE INTO user_badges (user_id, badge_id)
                SELECT ?, id FROM badges WHERE code = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, badgeCode);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to award badge", e);
        }
    }

    public void syncUserStats(Connection conn, Long userId) {
        String sql = """
                UPDATE users u
                SET topics_count = (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'),
                    posts_count = (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'),
                    likes_received_count = (
                        SELECT COUNT(*) FROM reactions r
                        WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'))
                           OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'))
                    )
                WHERE u.id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sync user badge stats", e);
        }
    }

    public int countTopicsByUser(Connection conn, Long userId) {
        return count(conn, "SELECT COUNT(*) FROM topics WHERE user_id = ? AND status <> 'DELETED'", userId);
    }

    public int countPostsByUser(Connection conn, Long userId) {
        return count(conn, "SELECT COUNT(*) FROM posts WHERE user_id = ? AND status <> 'DELETED'", userId);
    }

    public int countRepliesByUser(Connection conn, Long userId) {
        return count(conn, "SELECT COUNT(*) FROM posts WHERE user_id = ? AND post_number > 1 AND status <> 'DELETED'", userId);
    }

    public int countLikesReceivedByUser(Connection conn, Long userId) {
        String sql = """
                SELECT COUNT(*)
                FROM reactions r
                WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = ? AND t.status <> 'DELETED'))
                   OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = ? AND p.status <> 'DELETED'))
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count likes received", e);
        }
    }

    public int countUploadedFilesByUser(Connection conn, Long userId) {
        return count(conn, "SELECT COUNT(*) FROM uploaded_files WHERE user_id = ?", userId);
    }

    public List<Badge> listAutomaticRules(Connection conn) {
        ensureSchema();
        String sql = """
                SELECT b.*, NULL AS awarded_at,
                       (SELECT COUNT(*) FROM user_badges ub WHERE ub.badge_id = b.id) AS awarded_count
                FROM badges b
                WHERE b.rule_type <> 'MANUAL'
                ORDER BY b.sort_order ASC, b.id ASC
                """;
        List<Badge> badges = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                badges.add(map(rs));
            }
            return badges;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load automatic badge rules", e);
        }
    }

    private int count(Connection conn, String sql, Long userId) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate badge rule", e);
        }
    }

    private Badge map(ResultSet rs) throws Exception {
        Badge badge = new Badge();
        badge.setId(rs.getLong("id"));
        badge.setCode(rs.getString("code"));
        badge.setName(rs.getString("name"));
        badge.setDescription(rs.getString("description"));
        badge.setIcon(rs.getString("icon"));
        badge.setColor(rs.getString("color"));
        badge.setSortOrder(rs.getInt("sort_order"));
        badge.setRuleType(rs.getString("rule_type"));
        badge.setRuleThreshold(rs.getInt("rule_threshold"));
        badge.setAwardedCount(rs.getInt("awarded_count"));
        badge.setAwardedAt(DaoMapper.localDateTime(rs, "awarded_at"));
        return badge;
    }

    private int bindBadge(PreparedStatement ps, Badge badge) throws Exception {
        ps.setString(1, badge.getCode());
        ps.setString(2, badge.getName());
        ps.setString(3, badge.getDescription());
        ps.setString(4, badge.getIcon());
        ps.setString(5, badge.getColor());
        ps.setInt(6, badge.getSortOrder());
        ps.setString(7, badge.getRuleType());
        ps.setInt(8, badge.getRuleThreshold());
        return 9;
    }

    private void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (SCHEMA_LOCK) {
            if (schemaReady) {
                return;
            }
            try (Connection conn = Database.getConnection()) {
                boolean addedRuleType = ensureColumn(conn, "rule_type",
                        "ALTER TABLE badges ADD COLUMN rule_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL' AFTER sort_order");
                boolean addedRuleThreshold = ensureColumn(conn, "rule_threshold",
                        "ALTER TABLE badges ADD COLUMN rule_threshold INT NOT NULL DEFAULT 0 AFTER rule_type");
                if (addedRuleType || addedRuleThreshold) {
                    backfillDefaultRules(conn);
                }
                schemaReady = true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to ensure badge rule schema", e);
            }
        }
    }

    private boolean ensureColumn(Connection conn, String column, String alterSql) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, "badges", column)) {
            if (rs.next()) {
                return false;
            }
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(alterSql);
            return true;
        }
    }

    private void backfillDefaultRules(Connection conn) throws Exception {
        setRule(conn, "WELCOME", "REGISTRATION", 0);
        setRule(conn, "FIRST_TOPIC", "TOPICS", 1);
        setRule(conn, "FIRST_REPLY", "REPLIES", 1);
        setRule(conn, "TOPIC_BUILDER", "TOPICS", 5);
        setRule(conn, "ACTIVE_MEMBER", "POSTS", 10);
        setRule(conn, "LIKED", "LIKES_RECEIVED", 1);
        setRule(conn, "VALUABLE_POSTER", "LIKES_RECEIVED", 5);
        setRule(conn, "TRUSTED", "TRUST_LEVEL", 1);
        setRule(conn, "SHARER", "UPLOADS", 1);
    }

    private void setRule(Connection conn, String code, String ruleType, int threshold) throws Exception {
        String sql = """
                UPDATE badges
                SET rule_type = ?, rule_threshold = ?
                WHERE code = ? AND rule_type = 'MANUAL' AND rule_threshold = 0
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruleType);
            ps.setInt(2, threshold);
            ps.setString(3, code);
            ps.executeUpdate();
        }
    }
}
