package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.BookmarkDao;
import com.aitef.forum.model.dao.NotificationDao;
import com.aitef.forum.model.dao.PostDao;
import com.aitef.forum.model.dao.ReactionDao;
import com.aitef.forum.model.dao.TopicDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.dto.TopicCard;
import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.entity.UserPreference;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.Map;

public class InteractionService {
    private final ReactionDao reactionDao = new ReactionDao();
    private final BookmarkDao bookmarkDao = new BookmarkDao();
    private final TopicDao topicDao = new TopicDao();
    private final PostDao postDao = new PostDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final UserPreferenceService userPreferenceService = new UserPreferenceService();
    private final BadgeService badgeService = new BadgeService();

    public Map<String, Object> toggleTopicLike(Long userId, Long topicId) {
        TopicCard topic = topicDao.findCardById(topicId, userId)
                .orElseThrow(() -> new ValidationException("话题不存在"));
        return toggleReaction(userId, "TOPIC", topicId, topic.getUserId(), topic.getTitle(), "TOPIC", topicId);
    }

    public Map<String, Object> togglePostLike(Long userId, Long postId) {
        Post post = postDao.findById(postId)
                .orElseThrow(() -> new ValidationException("回复不存在"));
        if (post.getPostNumber() == 1) {
            return toggleTopicLike(userId, post.getTopicId());
        }
        return toggleReaction(userId, "POST", postId, post.getUserId(), "你的回复收到了赞", "TOPIC", post.getTopicId());
    }

    public Map<String, Object> toggleTopicBookmark(Long userId, Long topicId) {
        topicDao.findCardById(topicId, userId).orElseThrow(() -> new ValidationException("话题不存在"));
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean existed = bookmarkDao.exists(conn, userId, "TOPIC", topicId);
                if (existed) {
                    bookmarkDao.remove(conn, userId, "TOPIC", topicId);
                } else {
                    bookmarkDao.add(conn, userId, "TOPIC", topicId);
                }
                int count = bookmarkDao.refreshTopicCount(conn, topicId);
                conn.commit();
                return Map.of("active", !existed, "count", count);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to toggle bookmark", e);
        }
    }

    private Map<String, Object> toggleReaction(Long userId, String targetType, Long targetId, Long ownerId, String title, String notifyTargetType, Long notifyTargetId) {
        UserPreference ownerPreference = userPreferenceService.findByUserId(ownerId);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean existed = reactionDao.exists(conn, userId, targetType, targetId);
                if (existed) {
                    reactionDao.remove(conn, userId, targetType, targetId);
                    if (!userId.equals(ownerId)) {
                        userDao.incrementLikesReceived(conn, ownerId, -1);
                    }
                } else {
                    reactionDao.add(conn, userId, targetType, targetId);
                    if (!userId.equals(ownerId)) {
                        userDao.incrementLikesReceived(conn, ownerId, 1);
                    }
                    if (ownerPreference.isNotifyOnLike()) {
                        notificationDao.create(conn, ownerId, userId, "LIKE", notifyTargetType, notifyTargetId, "有人赞了你的内容", title);
                    }
                }
                int count = reactionDao.refreshCount(conn, targetType, targetId);
                if (!userId.equals(ownerId)) {
                    badgeService.refreshUserBadges(conn, ownerId);
                }
                conn.commit();
                return Map.of("active", !existed, "count", count);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to toggle reaction", e);
        }
    }
}
