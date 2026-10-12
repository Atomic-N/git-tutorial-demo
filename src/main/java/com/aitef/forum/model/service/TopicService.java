package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.NotificationDao;
import com.aitef.forum.model.dao.PostDao;
import com.aitef.forum.model.dao.TagDao;
import com.aitef.forum.model.dao.TopicDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.Category;
import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.dto.TopicCard;
import com.aitef.forum.model.dto.TopicDetail;
import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.entity.UserPreference;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.HtmlUtil;
import com.aitef.forum.util.MarkdownUtil;
import com.aitef.forum.util.SlugUtil;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicService {
    private final TopicDao topicDao = new TopicDao();
    private final PostDao postDao = new PostDao();
    private final TagDao tagDao = new TagDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final UserPreferenceService userPreferenceService = new UserPreferenceService();
    private final BadgeService badgeService = new BadgeService();
    private final TrustLevelService trustLevelService = new TrustLevelService();
    private final AuditLogService auditLogService = new AuditLogService();
    private final UserDao userDao = new UserDao();
    private final CatalogService catalogService = new CatalogService();
    private final SiteSettingService siteSettingService = new SiteSettingService();

    public Page<TopicCard> list(String mode, SearchCriteria criteria, Long authorId, Long currentUserId) {
        criteria.setViewerTrustLevel(currentUserId == null ? 0 : userDao.findById(currentUserId).map(u -> u.isAdmin() ? 99 : u.getTrustLevel()).orElse(0));
        if (currentUserId != null && !"unread".equals(mode) && !criteria.isHideReadTopics()) {
            UserPreference preference = userPreferenceService.findByUserId(currentUserId);
            criteria.setHideReadTopics(preference.isHideReadTopics());
        }
        return topicDao.list(mode, criteria, authorId, currentUserId);
    }

    public TopicDetail getDetail(Long topicId, Long currentUserId) {
        TopicCard topic = topicDao.findCardById(topicId, currentUserId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        topicDao.incrementViews(topicId);
        List<Post> posts = postDao.findByTopicId(topicId, currentUserId);
        posts.forEach(post -> post.setCookedContent(MarkdownUtil.normalizeCooked(post.getCookedContent())));
        topicDao.markRead(currentUserId, topicId, posts.isEmpty() ? 0 : posts.get(posts.size() - 1).getPostNumber());

        TopicDetail detail = new TopicDetail();
        detail.setTopic(topic);
        detail.setPosts(posts);
        detail.setTags(topic.getTags());
        return detail;
    }

    public TopicDetail getEditableDetail(Long topicId, Long actorId) {
        TopicCard topic = topicDao.findCardById(topicId, actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        User actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        if (!canManageTopic(actor, topic)) {
            throw new ValidationException("你无权编辑该话题");
        }
        TopicDetail detail = new TopicDetail();
        detail.setTopic(topic);
        List<Post> posts = postDao.findByTopicId(topicId, actorId);
        posts.forEach(post -> post.setCookedContent(MarkdownUtil.normalizeCooked(post.getCookedContent())));
        detail.setPosts(posts);
        detail.setTags(topic.getTags());
        return detail;
    }

    public void markAllRead(Long userId) {
        topicDao.markAllRead(userId);
    }

    public int unreadTopicCount(Long currentUserId, int viewerTrustLevel) {
        return topicDao.countUnreadTopics(currentUserId, viewerTrustLevel);
    }

    public Map<Long, Integer> unreadTopicCountsByCategory(Long currentUserId, int viewerTrustLevel) {
        return topicDao.countUnreadTopicsByCategory(currentUserId, viewerTrustLevel);
    }

    public Long createTopic(Long userId, Long categoryId, String title, String rawContent, String tagsText) {
        title = normalize(title);
        rawContent = normalize(rawContent);
        if (title.length() < 4 || title.length() > 180) {
            throw new ValidationException("话题标题长度需要在 4-180 个字符之间");
        }
        if (rawContent.length() < 5) {
            throw new ValidationException("话题内容至少需要 5 个字符");
        }
        if (categoryId == null) {
            throw new ValidationException("请选择分类");
        }
        User author = userDao.findById(userId).orElseThrow(() -> new ValidationException("用户不存在"));
        Category category = catalogService.findCategoryById(categoryId).orElseThrow(() -> new ValidationException("分类不存在"));
        if (!author.isAdmin() && author.getTrustLevel() < category.getMinTrustLevel()) {
            throw new ValidationException("当前信任等级无法在该分类发帖");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String slug = SlugUtil.fromTitle(title);
                String excerpt = HtmlUtil.excerpt(rawContent, 180);
                Long topicId = topicDao.create(conn, userId, categoryId, title, slug, excerpt);
                Long postId = postDao.create(conn, topicId, userId, null, 1, rawContent, cooked(rawContent));
                topicDao.setFirstPost(conn, topicId, postId);
                attachTags(conn, topicId, tagsText);
                incrementUserStats(conn, userId, true, true);
                trustLevelService.refresh(conn, userId);
                topicDao.refreshCategoryCount(conn, categoryId);
                badgeService.refreshUserBadges(conn, userId);
                auditLogService.log(conn, userId, "CREATE_TOPIC", "TOPIC", topicId, title);
                conn.commit();
                return topicId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create topic", e);
        }
    }

    public void updateTopic(Long actorId, Long topicId, Long categoryId, String title, String rawContent, String tagsText) {
        title = normalize(title);
        rawContent = normalize(rawContent);
        if (title.length() < 4 || title.length() > 180) {
            throw new ValidationException("话题标题长度需要在 4-180 个字符之间");
        }
        if (rawContent.length() < 5) {
            throw new ValidationException("话题内容至少需要 5 个字符");
        }
        if (categoryId == null) {
            throw new ValidationException("请选择分类");
        }
        User actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        TopicCard topic = topicDao.findCardById(topicId, actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        if (!canManageTopic(actor, topic)) {
            throw new ValidationException("你无权编辑该话题");
        }
        Category category = catalogService.findCategoryById(categoryId).orElseThrow(() -> new ValidationException("分类不存在"));
        if (!actor.getId().equals(topic.getUserId()) && !actor.canModerateCategory(categoryId)) {
            throw new ValidationException("你无权将话题调整到该分类");
        }
        if (!actor.isAdmin() && !actor.canModerateCategory(categoryId) && actor.getTrustLevel() < category.getMinTrustLevel()) {
            throw new ValidationException("当前信任等级无法将话题调整到该分类");
        }
        Post firstPost = postDao.findFirstByTopicId(topicId).orElseThrow(() -> new ValidationException("话题主楼不存在"));

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                topicDao.update(conn, topicId, categoryId, title, SlugUtil.fromTitle(title), HtmlUtil.excerpt(rawContent, 180));
                postDao.updateContent(conn, firstPost.getId(), rawContent, cooked(rawContent));
                syncTags(conn, topicId, tagsText);
                topicDao.refreshTopicAfterPostChange(conn, topicId);
                topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                if (!topic.getCategoryId().equals(categoryId)) {
                    topicDao.refreshCategoryCount(conn, categoryId);
                }
                auditLogService.log(conn, actorId, "UPDATE_TOPIC", "TOPIC", topicId, title);
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
            throw new IllegalStateException("Failed to update topic", e);
        }
    }

    public Long reply(Long userId, Long topicId, Long replyToPostId, String rawContent) {
        rawContent = normalize(rawContent);
        if (rawContent.length() < 2) {
            throw new ValidationException("回复内容至少需要 2 个字符");
        }
        TopicCard topic = topicDao.findCardById(topicId, userId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        if (replyToPostId != null) {
            Post replyTo = postDao.findById(replyToPostId)
                    .orElseThrow(() -> new ValidationException("被回复的内容不存在或已被删除"));
            if (!topicId.equals(replyTo.getTopicId())) {
                throw new ValidationException("只能回复当前话题中的帖子");
            }
        }
        UserPreference ownerPreference = userPreferenceService.findByUserId(topic.getUserId());

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int postNumber = postDao.nextPostNumber(conn, topicId);
                Long postId = postDao.create(conn, topicId, userId, replyToPostId, postNumber, rawContent, cooked(rawContent));
                topicDao.touchAfterReply(conn, topicId, postId);
                incrementUserStats(conn, userId, false, true);
                trustLevelService.refresh(conn, userId);
                if (ownerPreference.isNotifyOnReply()) {
                    notificationDao.create(conn, topic.getUserId(), userId, "REPLY", "TOPIC", topicId, "你的话题有新回复", topic.getTitle());
                }
                badgeService.refreshUserBadges(conn, userId);
                auditLogService.log(conn, userId, "CREATE_POST", "POST", postId, "topicId=" + topicId);
                conn.commit();
                return postId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create reply", e);
        }
    }

    public Post getEditablePost(Long postId, Long actorId) {
        Post post = postDao.findById(postId).orElseThrow(() -> new ValidationException("回复不存在"));
        if (post.getPostNumber() == 1) {
            throw new ValidationException("主楼请通过编辑话题完成修改");
        }
        User actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        TopicCard topic = topicDao.findCardById(post.getTopicId(), actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        if (!canManageReply(actor, topic, post)) {
            throw new ValidationException("你无权编辑该回复");
        }
        return post;
    }

    public void updateReply(Long actorId, Long postId, String rawContent) {
        rawContent = normalize(rawContent);
        if (rawContent.length() < 2) {
            throw new ValidationException("回复内容至少需要 2 个字符");
        }
        Post post = getEditablePost(postId, actorId);
        try (Connection conn = Database.getConnection()) {
            postDao.updateContent(conn, postId, rawContent, cooked(rawContent));
            auditLogService.log(conn, actorId, "UPDATE_POST", "POST", postId, "topicId=" + post.getTopicId());
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update reply", e);
        }
    }

    public void deleteTopic(Long actorId, Long topicId) {
        User actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        TopicCard topic = topicDao.findCardById(topicId, actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        if (!canManageTopic(actor, topic)) {
            throw new ValidationException("你无权删除该话题");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                topicDao.softDelete(conn, topicId);
                topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                auditLogService.log(conn, actorId, "DELETE_TOPIC", "TOPIC", topicId, topic.getTitle());
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

    public void deleteReply(Long actorId, Long postId) {
        User actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        Post post = postDao.findById(postId).orElseThrow(() -> new ValidationException("回复不存在"));
        if (post.getPostNumber() == 1) {
            throw new ValidationException("主楼请通过删除话题处理");
        }
        TopicCard topic = topicDao.findCardById(post.getTopicId(), actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        if (!canManageReply(actor, topic, post)) {
            throw new ValidationException("你无权删除该回复");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                postDao.softDelete(conn, postId);
                topicDao.refreshTopicAfterPostChange(conn, topic.getId());
                topicDao.refreshCategoryCount(conn, topic.getCategoryId());
                auditLogService.log(conn, actorId, "DELETE_POST", "POST", postId, "topicId=" + topic.getId());
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
            throw new IllegalStateException("Failed to delete reply", e);
        }
    }

    private void attachTags(Connection conn, Long topicId, String tagsText) {
        Set<Long> tagIds = new LinkedHashSet<>();
        if (tagsText != null) {
            for (String raw : tagsText.split("[,，\\s]+")) {
                if (tagIds.size() >= 5) {
                    break;
                }
                Long tagId = tagDao.findOrCreate(conn, raw);
                if (tagId != null) {
                    tagIds.add(tagId);
                }
            }
        }
        for (Long tagId : tagIds) {
            tagDao.attachTag(conn, topicId, tagId);
            tagDao.refreshTopicCount(conn, tagId);
        }
    }

    public String tagText(Long topicId) {
        return tagDao.findByTopicId(topicId).stream()
                .map(tag -> tag.getName())
                .collect(Collectors.joining(" "));
    }

    private void syncTags(Connection conn, Long topicId, String tagsText) {
        Set<Long> oldTagIds = new LinkedHashSet<>(tagDao.findTagIdsByTopicId(conn, topicId));
        Set<Long> newTagIds = resolveTagIds(conn, tagsText);
        tagDao.clearTopicTags(conn, topicId);
        for (Long tagId : newTagIds) {
            tagDao.attachTag(conn, topicId, tagId);
        }
        Set<Long> all = new LinkedHashSet<>(oldTagIds);
        all.addAll(newTagIds);
        for (Long tagId : all) {
            tagDao.refreshTopicCount(conn, tagId);
        }
    }

    private Set<Long> resolveTagIds(Connection conn, String tagsText) {
        Set<Long> tagIds = new LinkedHashSet<>();
        if (tagsText == null) {
            return tagIds;
        }
        for (String raw : tagsText.split("[,，\\s]+")) {
            if (tagIds.size() >= 5) {
                break;
            }
            Long tagId = tagDao.findOrCreate(conn, raw);
            if (tagId != null) {
                tagIds.add(tagId);
            }
        }
        return tagIds;
    }

    private void incrementUserStats(Connection conn, Long userId, boolean topic, boolean post) {
        String sql = """
                UPDATE users
                SET topics_count = topics_count + ?, posts_count = posts_count + ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topic ? 1 : 0);
            ps.setInt(2, post ? 1 : 0);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update user stats", e);
        }
    }

    public void markSolution(Long topicId, Long postId, Long actorId, boolean solved) {
        TopicCard topic = topicDao.findCardById(topicId, actorId)
                .orElseThrow(() -> new ValidationException("话题不存在或已被删除"));
        var post = postDao.findById(postId).orElseThrow(() -> new ValidationException("回复不存在"));
        var actor = userDao.findById(actorId).orElseThrow(() -> new ValidationException("用户不存在"));
        if (!post.getTopicId().equals(topicId) || post.getPostNumber() == 1) {
            throw new ValidationException("只能将该话题中的回复标记为已解决");
        }
        if (!canManageTopic(actor, topic)) {
            throw new ValidationException("只有话题作者、版主或管理员可以标记已解决");
        }
        topicDao.updateSolution(topicId, solved ? postId : null, solved);
    }

    private boolean canManageTopic(User actor, TopicCard topic) {
        if (actor == null || topic == null) {
            return false;
        }
        return actor.getId().equals(topic.getUserId()) || actor.canModerateCategory(topic.getCategoryId());
    }

    private boolean canManageReply(User actor, TopicCard topic, Post post) {
        if (actor == null || topic == null || post == null) {
            return false;
        }
        return actor.getId().equals(post.getUserId()) || actor.canModerateCategory(topic.getCategoryId());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String cooked(String rawContent) {
        return siteSettingService.booleanValue("markdown_enabled")
                ? HtmlUtil.toCookedContent(rawContent)
                : "<p>" + HtmlUtil.escape(rawContent).replace("\n", "<br>") + "</p>";
    }
}
