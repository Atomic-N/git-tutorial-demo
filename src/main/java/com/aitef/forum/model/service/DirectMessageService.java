package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.DirectMessageDao;
import com.aitef.forum.model.dao.NotificationDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.DirectMessage;
import com.aitef.forum.model.entity.DirectMessageThread;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.HtmlUtil;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;

public class DirectMessageService {
    private final DirectMessageDao directMessageDao = new DirectMessageDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final UserRelationshipService relationshipService = new UserRelationshipService();
    private final AuditLogService auditLogService = new AuditLogService();
    private final SiteSettingService siteSettingService = new SiteSettingService();

    public List<DirectMessageThread> listThreads(Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        return directMessageDao.listThreads(userId);
    }

    public DirectMessageThread getThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        DirectMessageThread thread = directMessageDao.findThreadById(threadId, userId);
        if (thread == null) {
            throw new ValidationException("私信会话不存在");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.markRead(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark direct message read", e);
        }
        return thread;
    }

    public List<DirectMessage> listMessages(Long threadId, Long userId) {
        getThread(threadId, userId);
        return directMessageDao.listMessages(threadId);
    }

    public int countUnreadThreads(Long userId) {
        return userId == null ? 0 : directMessageDao.countUnreadThreads(userId);
    }

    public Long sendToUser(Long senderId, String targetUsername, String subject, String body) {
        return sendToUser(senderId, targetUsername, subject, body, null);
    }

    public Long sendToUser(Long senderId, String targetUsername, String subject, String body, Long quotedMessageId) {
        if (!siteSettingService.booleanValue("allow_private_messages")) {
            throw new ValidationException("站点当前未开启私信功能");
        }
        User sender = userDao.findById(senderId).orElseThrow(() -> new ValidationException("用户不存在"));
        User target = userDao.findByUsername(targetUsername).orElseThrow(() -> new ValidationException("目标用户不存在"));
        if (relationshipService.state(target.getId(), sender.getId()).get("blocked")) {
            throw new ValidationException("对方当前不接收你的私信");
        }
        String raw = body == null ? "" : body.trim();
        if (raw.length() < 2 || raw.length() > 4000) {
            throw new ValidationException("私信内容长度需要在 2-4000 个字符之间");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long threadId = directMessageDao.findThreadBetween(conn, sender.getId(), target.getId())
                        .orElseGet(() -> {
                            Long created = directMessageDao.createThread(conn, subject == null ? "" : subject.trim());
                            directMessageDao.addParticipant(conn, created, sender.getId());
                            directMessageDao.addParticipant(conn, created, target.getId());
                            return created;
                        });
                Long messageId = directMessageDao.createMessage(conn, threadId, sender.getId(), raw, cooked(raw), quotedMessageId);
                directMessageDao.touchThread(conn, threadId);
                directMessageDao.markRead(conn, threadId, sender.getId());
                notificationDao.create(conn, target.getId(), sender.getId(), "DIRECT_MESSAGE", "DM_THREAD", threadId, "你收到了新的私信", sender.getDisplayName());
                auditLogService.log(conn, sender.getId(), "SEND_DIRECT_MESSAGE", "DM_THREAD", threadId, "messageId=" + messageId);
                conn.commit();
                return threadId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send direct message", e);
        }
    }

    private String cooked(String raw) {
        return siteSettingService.booleanValue("markdown_enabled")
                ? HtmlUtil.toCookedContent(raw)
                : "<p>" + HtmlUtil.escape(raw).replace("\n", "<br>") + "</p>";
    }

    // ==================== 会话管理功能 ====================

    public void pinThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.pinThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to pin thread", e);
        }
    }

    public void unpinThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.unpinThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unpin thread", e);
        }
    }

    public void archiveThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.archiveThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive thread", e);
        }
    }

    public void unarchiveThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.unarchiveThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unarchive thread", e);
        }
    }

    public void muteThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.muteThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mute thread", e);
        }
    }

    public void unmuteThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.unmuteThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unmute thread", e);
        }
    }

    public void deleteThread(Long threadId, Long userId) {
        if (userId == null) {
            throw new ValidationException("请先登录");
        }
        try (Connection conn = Database.getConnection()) {
            directMessageDao.deleteThread(conn, threadId, userId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete thread", e);
        }
    }

}
