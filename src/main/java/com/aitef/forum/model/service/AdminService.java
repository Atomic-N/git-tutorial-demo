package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.AdminDao;
import com.aitef.forum.model.dao.PostDao;
import com.aitef.forum.model.dao.TopicDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.dto.AdminStats;
import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.dto.TopicCard;
import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminService {
    private final AdminDao adminDao = new AdminDao();
    private final TopicDao topicDao = new TopicDao();
    private final PostDao postDao = new PostDao();
    private final UserDao userDao = new UserDao();
    private final BadgeService badgeService = new BadgeService();
    private final AuditLogService auditLogService = new AuditLogService();

    public void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new ValidationException("需要管理员权限");
        }
    }

    public AdminStats stats() {
        return adminDao.stats();
    }

    public Page<TopicCard> recentTopics(int page, Long currentUserId) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(page);
        criteria.setSize(12);
        criteria.setViewerTrustLevel(99);
        return topicDao.list("new", criteria, null, currentUserId);
    }

    public Page<TopicCard> adminTopics(SearchCriteria criteria, Long currentUserId) {
        criteria.setViewerTrustLevel(99);
        criteria.setSize(Math.min(30, Math.max(8, criteria.getSize())));
        return topicDao.list("latest", criteria, null, currentUserId);
    }

    public Page<User> adminUsers(int page, String keyword, String status, String role, Integer trustLevel) {
        return userDao.pageForAdmin(page, 18, keyword, status, role, trustLevel);
    }

    public Page<User> adminUsers(int page) {
        return adminUsers(page, "", "", "", null);
    }

    public void deleteTopic(Long actorId, Long topicId) {
        TopicCard topic = topicDao.findCardById(topicId, null, 99)
                .orElseThrow(() -> new ValidationException("话题不存在"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                topicDao.softDelete(conn, topicId);
                topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                auditLogService.log(conn, actorId, "ADMIN_DELETE_TOPIC", "TOPIC", topicId, topic.getTitle());
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
            throw new IllegalStateException("Failed to delete topic", e);
        }
    }

    public Long deletePost(Long actorId, Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ValidationException("回复不存在"));
        TopicCard topic = topicDao.findCardById(post.getTopicId(), null, 99)
                .orElseThrow(() -> new ValidationException("话题不存在"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                postDao.softDelete(conn, postId);
                topicDao.refreshTopicAfterPostChange(conn, post.getTopicId());
                topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                auditLogService.log(conn, actorId, "ADMIN_DELETE_POST", "POST", postId, "topicId=" + post.getTopicId());
                conn.commit();
                return post.getTopicId();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete post", e);
        }
    }

    public void pinTopic(Long actorId, Long topicId, boolean pinned) {
        try (Connection conn = Database.getConnection()) {
            topicDao.updatePinned(conn, topicId, pinned);
            auditLogService.log(conn, actorId, pinned ? "ADMIN_PIN_TOPIC" : "ADMIN_UNPIN_TOPIC", "TOPIC", topicId, "");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic pin status", e);
        }
    }

    public int bulkUpdateTopics(Long actorId, List<Long> topicIds, String action) {
        List<Long> ids = (topicIds == null ? List.<Long>of() : topicIds).stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .limit(100)
                .toList();
        if (ids.isEmpty()) {
            throw new ValidationException("请选择要处理的话题");
        }
        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (!List.of("pin", "unpin", "delete").contains(normalizedAction)) {
            throw new ValidationException("批量操作不正确");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int count = 0;
                for (Long topicId : ids) {
                    TopicCard topic = topicDao.findCardById(topicId, null, 99).orElse(null);
                    if (topic == null) {
                        continue;
                    }
                    if ("delete".equals(normalizedAction)) {
                        topicDao.softDelete(conn, topicId);
                        topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                    } else {
                        boolean pinned = "pin".equals(normalizedAction);
                        topicDao.updatePinned(conn, topicId, pinned);
                    }
                    auditLogService.log(conn, actorId, "ADMIN_BULK_TOPIC_" + normalizedAction.toUpperCase(Locale.ROOT),
                            "TOPIC", topicId, topic.getTitle());
                    count++;
                }
                conn.commit();
                return count;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topics in batch", e);
        }
    }

    public void moderateUser(Long actorId, Long userId, String status, int trustLevel) {
        if (userId == null) {
            throw new ValidationException("用户不存在");
        }
        String normalizedStatus = status == null ? "ACTIVE" : status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalizedStatus) && !"SUSPENDED".equals(normalizedStatus)) {
            throw new ValidationException("用户状态不正确");
        }
        int safeTrust = Math.max(0, Math.min(4, trustLevel));
        try (Connection conn = Database.getConnection()) {
            userDao.updateStatusAndTrust(conn, userId, normalizedStatus, safeTrust);
            auditLogService.log(conn, actorId, "ADMIN_MODERATE_USER", "USER", userId, "status=" + normalizedStatus + ",trust=" + safeTrust);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update user moderation", e);
        }
    }

    public int refreshBadges() {
        return badgeService.refreshAllUsers();
    }
}
