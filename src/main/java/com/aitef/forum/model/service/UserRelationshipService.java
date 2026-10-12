package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.NotificationDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.dao.UserRelationshipDao;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class UserRelationshipService {
    private final UserRelationshipDao relationshipDao = new UserRelationshipDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final AuditLogService auditLogService = new AuditLogService();

    public Map<String, Boolean> state(Long viewerId, Long profileId) {
        if (viewerId == null || profileId == null || viewerId.equals(profileId)) {
            return Map.of("following", false, "muted", false, "blocked", false);
        }
        return Map.of(
                "following", relationshipDao.exists(viewerId, profileId, "FOLLOW"),
                "muted", relationshipDao.exists(viewerId, profileId, "MUTE"),
                "blocked", relationshipDao.exists(viewerId, profileId, "BLOCK")
        );
    }

    public void follow(Long actorId, Long targetUserId) {
        validate(actorId, targetUserId);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureAllowed(conn, actorId, targetUserId);
                relationshipDao.add(actorId, targetUserId, "FOLLOW");
                notificationDao.create(conn, targetUserId, actorId, "FOLLOW", "USER", targetUserId, "你有新的关注者", userDao.findById(conn, actorId).map(User::getDisplayName).orElse("用户"));
                auditLogService.log(conn, actorId, "FOLLOW_USER", "USER", targetUserId, "");
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
            throw new IllegalStateException("Failed to follow user", e);
        }
    }

    public void unfollow(Long actorId, Long targetUserId) {
        validate(actorId, targetUserId);
        relationshipDao.remove(actorId, targetUserId, "FOLLOW");
        auditLogService.log(actorId, "UNFOLLOW_USER", "USER", targetUserId, "");
    }

    public void mute(Long actorId, Long targetUserId, boolean muted) {
        validate(actorId, targetUserId);
        if (muted) {
            relationshipDao.add(actorId, targetUserId, "MUTE");
            auditLogService.log(actorId, "MUTE_USER", "USER", targetUserId, "");
        } else {
            relationshipDao.remove(actorId, targetUserId, "MUTE");
            auditLogService.log(actorId, "UNMUTE_USER", "USER", targetUserId, "");
        }
    }

    public void block(Long actorId, Long targetUserId, boolean blocked) {
        validate(actorId, targetUserId);
        if (blocked) {
            relationshipDao.remove(actorId, targetUserId, "FOLLOW");
            relationshipDao.add(actorId, targetUserId, "BLOCK");
            auditLogService.log(actorId, "BLOCK_USER", "USER", targetUserId, "");
        } else {
            relationshipDao.remove(actorId, targetUserId, "BLOCK");
            auditLogService.log(actorId, "UNBLOCK_USER", "USER", targetUserId, "");
        }
    }

    public List<User> following(Long userId) {
        return relationshipDao.listTargets(userId, "FOLLOW");
    }

    private void validate(Long actorId, Long targetUserId) {
        if (actorId == null) {
            throw new ValidationException("请先登录");
        }
        if (targetUserId == null) {
            throw new ValidationException("目标用户不存在");
        }
        if (actorId.equals(targetUserId)) {
            throw new ValidationException("不能对自己执行该操作");
        }
    }

    private void ensureAllowed(Connection conn, Long actorId, Long targetUserId) {
        userDao.findById(conn, actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        userDao.findById(conn, targetUserId).orElseThrow(() -> new ValidationException("目标用户不存在"));
        if (relationshipDao.findFirstBlockingType(targetUserId, actorId).isPresent()) {
            throw new ValidationException("你当前无法与该用户建立关系");
        }
    }
}
