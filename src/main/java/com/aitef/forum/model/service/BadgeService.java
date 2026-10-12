package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.BadgeDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.Badge;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BadgeService {
    private static final Set<String> RULE_TYPES = Set.of(
            "MANUAL",
            "REGISTRATION",
            "TOPICS",
            "REPLIES",
            "POSTS",
            "LIKES_RECEIVED",
            "TRUST_LEVEL",
            "UPLOADS"
    );

    private final BadgeDao badgeDao = new BadgeDao();
    private final UserDao userDao = new UserDao();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Badge> listAll() {
        return badgeDao.listAll();
    }

    public List<Badge> listForAdmin() {
        return badgeDao.listAll();
    }

    public List<Badge> listCatalogForUser(Long userId) {
        if (userId != null) {
            refreshUserBadges(userId);
        }
        return badgeDao.listCatalogForUser(userId);
    }

    public List<Badge> listByUserId(Long userId) {
        refreshUserBadges(userId);
        return badgeDao.listByUserId(userId);
    }

    public int refreshAllUsers() {
        int count = 0;
        for (Long userId : badgeDao.listUserIds()) {
            refreshUserBadges(userId);
            count++;
        }
        return count;
    }

    public void refreshUserBadges(Long userId) {
        if (userId == null) {
            return;
        }
        try (Connection conn = Database.getConnection()) {
            refreshUserBadges(conn, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh user badges", e);
        }
    }

    public Long createBadge(Long actorId, Badge input) {
        Badge badge = normalizeBadge(input, false);
        if (badgeDao.existsByCodeExceptId(badge.getCode(), null)) {
            throw new ValidationException("徽章代码已存在");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long badgeId = badgeDao.create(conn, badge);
                auditLogService.log(conn, actorId, "BADGE_CREATE", "BADGE", badgeId, badge.getCode());
                conn.commit();
                return badgeId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create badge", e);
        }
    }

    public void updateBadge(Long actorId, Badge input) {
        Badge badge = normalizeBadge(input, true);
        Badge existing = badgeDao.findById(badge.getId())
                .orElseThrow(() -> new ValidationException("徽章不存在"));
        if (badgeDao.existsByCodeExceptId(badge.getCode(), badge.getId())) {
            throw new ValidationException("徽章代码已存在");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                badgeDao.update(conn, badge);
                auditLogService.log(conn, actorId, "BADGE_UPDATE", "BADGE", badge.getId(),
                        existing.getCode() + " -> " + badge.getCode());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update badge", e);
        }
    }

    public void deleteBadge(Long actorId, Long badgeId) {
        if (badgeId == null) {
            throw new ValidationException("徽章不存在");
        }
        Badge existing = badgeDao.findById(badgeId)
                .orElseThrow(() -> new ValidationException("徽章不存在"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                badgeDao.delete(conn, badgeId);
                auditLogService.log(conn, actorId, "BADGE_DELETE", "BADGE", badgeId, existing.getCode());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete badge", e);
        }
    }

    public void awardBadge(Long actorId, Long badgeId, String username) {
        User user = findTargetUser(username);
        Badge badge = findBadgeOrThrow(badgeId);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                badgeDao.awardToUser(conn, user.getId(), badge.getId());
                auditLogService.log(conn, actorId, "BADGE_AWARD", "USER", user.getId(), badge.getCode());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to award badge", e);
        }
    }

    public void revokeBadge(Long actorId, Long badgeId, String username) {
        User user = findTargetUser(username);
        Badge badge = findBadgeOrThrow(badgeId);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                badgeDao.revokeFromUser(conn, user.getId(), badge.getId());
                auditLogService.log(conn, actorId, "BADGE_REVOKE", "USER", user.getId(), badge.getCode());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to revoke badge", e);
        }
    }

    public void refreshUserBadges(Connection conn, Long userId) {
        try {
            badgeDao.syncUserStats(conn, userId);
            User user = userDao.findById(conn, userId).orElse(null);
            if (user == null) {
                return;
            }
            int topics = badgeDao.countTopicsByUser(conn, userId);
            int posts = badgeDao.countPostsByUser(conn, userId);
            int replies = badgeDao.countRepliesByUser(conn, userId);
            int likesReceived = badgeDao.countLikesReceivedByUser(conn, userId);
            int uploads = badgeDao.countUploadedFilesByUser(conn, userId);
            BadgeStats stats = new BadgeStats(topics, posts, replies, likesReceived, uploads);

            for (Badge badge : badgeDao.listAutomaticRules(conn)) {
                if (matchesRule(badge, user, stats)) {
                    badgeDao.awardToUser(conn, userId, badge.getId());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to award user badges", e);
        }
    }

    private User findTargetUser(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isBlank()) {
            throw new ValidationException("请输入用户名");
        }
        return userDao.findByUsername(normalized)
                .orElseThrow(() -> new ValidationException("用户不存在"));
    }

    private Badge findBadgeOrThrow(Long badgeId) {
        return badgeDao.findById(badgeId)
                .orElseThrow(() -> new ValidationException("徽章不存在"));
    }

    private Badge normalizeBadge(Badge input, boolean requireId) {
        if (input == null) {
            throw new ValidationException("徽章信息不完整");
        }
        if (requireId && input.getId() == null) {
            throw new ValidationException("徽章不存在");
        }

        String code = normalizeCode(input.getCode());
        if (!code.matches("[A-Z0-9_]{2,40}")) {
            throw new ValidationException("徽章代码只能使用 2-40 位大写字母、数字或下划线");
        }

        String name = text(input.getName());
        if (name.length() < 2 || name.length() > 60) {
            throw new ValidationException("徽章名称长度需要在 2-60 个字符之间");
        }

        String description = text(input.getDescription());
        if (description.length() > 255) {
            throw new ValidationException("徽章说明最多 255 个字符");
        }

        String icon = text(input.getIcon()).toLowerCase(Locale.ROOT);
        if (icon.isBlank()) {
            icon = "award";
        }
        if (!icon.matches("[a-z0-9-]{2,40}")) {
            throw new ValidationException("图标名称只能使用小写字母、数字或短横线");
        }

        String color = text(input.getColor());
        if (color.isBlank()) {
            color = "#0ea5e9";
        }
        if (!color.matches("#[0-9a-fA-F]{6}")) {
            throw new ValidationException("颜色必须是 #RRGGBB 格式");
        }

        Badge badge = new Badge();
        badge.setId(input.getId());
        badge.setCode(code);
        badge.setName(name);
        badge.setDescription(description);
        badge.setIcon(icon);
        badge.setColor(color.toLowerCase(Locale.ROOT));
        badge.setSortOrder(Math.max(0, Math.min(9999, input.getSortOrder())));
        badge.setRuleType(normalizeRuleType(input.getRuleType()));
        badge.setRuleThreshold(normalizeRuleThreshold(badge.getRuleType(), input.getRuleThreshold()));
        return badge;
    }

    private boolean matchesRule(Badge badge, User user, BadgeStats stats) {
        int threshold = badge.getRuleThreshold();
        return switch (badge.getRuleType()) {
            case "REGISTRATION" -> true;
            case "TOPICS" -> stats.topics() >= threshold;
            case "REPLIES" -> stats.replies() >= threshold;
            case "POSTS" -> stats.posts() >= threshold;
            case "LIKES_RECEIVED" -> stats.likesReceived() >= threshold;
            case "TRUST_LEVEL" -> user.getTrustLevel() >= threshold;
            case "UPLOADS" -> stats.uploads() >= threshold;
            default -> false;
        };
    }

    private String normalizeRuleType(String ruleType) {
        String normalized = text(ruleType).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "MANUAL";
        }
        if (!RULE_TYPES.contains(normalized)) {
            throw new ValidationException("自动条件类型不支持");
        }
        return normalized;
    }

    private int normalizeRuleThreshold(String ruleType, int threshold) {
        if ("MANUAL".equals(ruleType) || "REGISTRATION".equals(ruleType)) {
            return 0;
        }
        if (threshold <= 0) {
            throw new ValidationException("自动条件阈值需要大于 0");
        }
        return Math.min(threshold, 9999);
    }

    private String normalizeCode(String code) {
        return text(code)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("\\s+", "_");
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private record BadgeStats(int topics, int posts, int replies, int likesReceived, int uploads) {
    }
}
