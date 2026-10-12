package com.aitef.forum.model.dao;

import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.dto.TopicCard;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TopicDao {
    private final TagDao tagDao = new TagDao();

    public Page<TopicCard> list(String mode, SearchCriteria criteria, Long authorId, Long currentUserId) {
        int page = Math.max(1, criteria.getPage());
        int size = Math.min(50, Math.max(5, criteria.getSize()));
        Query query = buildQuery(mode, criteria, authorId, currentUserId, false);
        Query countQuery = buildQuery(mode, criteria, authorId, currentUserId, true);

        List<TopicCard> topics = new ArrayList<>();
        long total;
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement countPs = conn.prepareStatement(countQuery.sql)) {
                bind(countPs, countQuery.params);
                try (ResultSet rs = countPs.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(query.sql)) {
                int index = bind(ps, query.params);
                ps.setInt(index++, size);
                ps.setInt(index, (page - 1) * size);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        topics.add(mapCard(rs));
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list topics", e);
        }

        attachTags(topics);
        return new Page<>(topics, page, size, total);
    }

    public Optional<TopicCard> findCardById(Long id, Long currentUserId) {
        int trustLevel = 0;
        if (currentUserId != null) {
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT trust_level, role FROM users WHERE id = ?")) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        trustLevel = "ADMIN".equalsIgnoreCase(rs.getString("role")) ? 99 : rs.getInt("trust_level");
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read viewer trust level", e);
            }
        }
        return findCardById(id, currentUserId, trustLevel);
    }

    public Optional<TopicCard> findCardById(Long id, Long currentUserId, int trustLevel) {
        String sql = """
                SELECT t.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                       c.name AS category_name, c.slug AS category_slug, c.color AS category_color, c.icon AS category_icon,
                       CASE WHEN lr.id IS NULL THEN FALSE ELSE TRUE END AS liked_by_current_user,
                       CASE WHEN bm.id IS NULL THEN FALSE ELSE TRUE END AS bookmarked_by_current_user,
                       ((t.likes_count * 5) + (t.replies_count * 4) + (t.bookmarks_count * 6) + (LOG10(t.views_count + 1) * 8)) /
                           POW(TIMESTAMPDIFF(HOUR, t.created_at, NOW()) + 2, 0.8) AS hot_score,
                       CASE WHEN ? = 0 THEN FALSE WHEN tr_card.topic_id IS NULL OR tr_card.last_read_at < t.last_post_at THEN TRUE ELSE FALSE END AS unread_by_current_user,
                       CASE WHEN ? = 0 THEN 0 ELSE GREATEST(0, t.replies_count + 1 - COALESCE(tr_card.last_read_post_number, 0)) END AS unread_posts_count
                FROM topics t
                JOIN users u ON u.id = t.user_id
                JOIN categories c ON c.id = t.category_id
                LEFT JOIN reactions lr ON lr.target_type = 'TOPIC' AND lr.target_id = t.id AND lr.user_id = ?
                LEFT JOIN bookmarks bm ON bm.target_type = 'TOPIC' AND bm.target_id = t.id AND bm.user_id = ?
                LEFT JOIN topic_reads tr_card ON tr_card.topic_id = t.id AND tr_card.user_id = ?
                WHERE t.id = ? AND t.status <> 'DELETED' AND c.min_trust_level <= ?
                """;
        long viewerId = currentUserId == null ? 0L : currentUserId;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, viewerId);
            ps.setLong(2, viewerId);
            ps.setLong(3, viewerId);
            ps.setLong(4, viewerId);
            ps.setLong(5, viewerId);
            ps.setLong(6, id);
            ps.setInt(7, trustLevel);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                TopicCard topic = mapCard(rs);
                topic.setTags(tagDao.findByTopicId(topic.getId()));
                return Optional.of(topic);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find topic", e);
        }
    }

    public Long create(Connection conn, Long userId, Long categoryId, String title, String slug, String excerpt) {
        String sql = """
                INSERT INTO topics (user_id, category_id, title, slug, excerpt, last_post_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.setString(3, title);
            ps.setString(4, slug);
            ps.setString(5, excerpt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for topic");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create topic", e);
        }
    }

    public void setFirstPost(Connection conn, Long topicId, Long postId) {
        String sql = "UPDATE topics SET last_post_id = ?, last_post_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic first post", e);
        }
    }

    public void update(Connection conn, Long topicId, Long categoryId, String title, String slug, String excerpt) {
        String sql = """
                UPDATE topics
                SET category_id = ?, title = ?, slug = ?, excerpt = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            ps.setString(2, title);
            ps.setString(3, slug);
            ps.setString(4, excerpt);
            ps.setLong(5, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic", e);
        }
    }

    public void touchAfterReply(Connection conn, Long topicId, Long postId) {
        String sql = """
                UPDATE topics
                SET replies_count = GREATEST(0, replies_count + 1), last_post_id = ?, last_post_at = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic after reply", e);
        }
    }

    public void incrementViews(Long topicId) {
        String sql = "UPDATE topics SET views_count = views_count + 1 WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to increment topic views", e);
        }
    }

    public void markRead(Long userId, Long topicId, int postNumber) {
        if (userId == null) {
            return;
        }
        String sql = """
                INSERT INTO topic_reads (user_id, topic_id, last_read_post_number, last_read_at)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE last_read_post_number = GREATEST(last_read_post_number, VALUES(last_read_post_number)), last_read_at = NOW()
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, topicId);
            ps.setInt(3, postNumber);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark topic read", e);
        }
    }

    public void markAllRead(Long userId) {
        if (userId == null) {
            return;
        }
        String sql = """
                INSERT INTO topic_reads (user_id, topic_id, last_read_post_number, last_read_at)
                SELECT ?, t.id,
                       COALESCE((SELECT MAX(p.post_number) FROM posts p WHERE p.topic_id = t.id AND p.status <> 'DELETED'), 0),
                       NOW()
                FROM topics t
                WHERE t.status <> 'DELETED'
                ON DUPLICATE KEY UPDATE last_read_post_number = VALUES(last_read_post_number), last_read_at = NOW()
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark all topics read", e);
        }
    }

    public void refreshCategoryCount(Connection conn, Long categoryId) {
        String sql = """
                UPDATE categories c
                SET topic_count = (SELECT COUNT(*) FROM topics t WHERE t.category_id = c.id AND t.status <> 'DELETED'),
                    post_count = (
                        SELECT COUNT(*)
                        FROM posts p
                        JOIN topics t ON t.id = p.topic_id
                        WHERE t.category_id = c.id AND t.status <> 'DELETED' AND p.status <> 'DELETED'
                    )
                WHERE c.id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh category count", e);
        }
    }

    public void refreshTopicAfterPostChange(Connection conn, Long topicId) {
        String sql = """
                UPDATE topics t
                SET replies_count = GREATEST(0, (
                        SELECT COUNT(*)
                        FROM posts p
                        WHERE p.topic_id = t.id AND p.status <> 'DELETED'
                    ) - 1),
                    last_post_id = (
                        SELECT p.id
                        FROM posts p
                        WHERE p.topic_id = t.id AND p.status <> 'DELETED'
                        ORDER BY p.post_number DESC
                        LIMIT 1
                    ),
                    last_post_at = COALESCE((
                        SELECT p.created_at
                        FROM posts p
                        WHERE p.topic_id = t.id AND p.status <> 'DELETED'
                        ORDER BY p.post_number DESC
                        LIMIT 1
                    ), t.created_at),
                    solution_post_id = CASE
                        WHEN t.solution_post_id IS NOT NULL AND EXISTS (
                            SELECT 1 FROM posts sp WHERE sp.id = t.solution_post_id AND sp.status <> 'DELETED'
                        ) THEN t.solution_post_id
                        ELSE NULL
                    END,
                    solved = CASE
                        WHEN t.solution_post_id IS NOT NULL AND EXISTS (
                            SELECT 1 FROM posts sp WHERE sp.id = t.solution_post_id AND sp.status <> 'DELETED'
                        ) THEN t.solved
                        ELSE FALSE
                    END
                WHERE t.id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh topic state", e);
        }
    }

    public void softDelete(Connection conn, Long topicId) {
        String sql = "UPDATE topics SET status = 'DELETED' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete topic", e);
        }
    }

    public void updatePinned(Connection conn, Long topicId, boolean pinned) {
        String sql = "UPDATE topics SET pinned = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic pin status", e);
        }
    }

    public void updateSolution(Long topicId, Long solutionPostId, boolean solved) {
        String sql = "UPDATE topics SET solved = ?, solution_post_id = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, solved);
            if (solutionPostId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, solutionPostId);
            }
            ps.setLong(3, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update topic solution", e);
        }
    }

    public int countUnreadTopics(Long userId, int viewerTrustLevel) {
        if (userId == null) {
            return 0;
        }
        String sql = """
                SELECT COUNT(*)
                FROM topics t
                JOIN categories c ON c.id = t.category_id
                LEFT JOIN topic_reads tr ON tr.topic_id = t.id AND tr.user_id = ?
                WHERE t.status <> 'DELETED'
                  AND c.min_trust_level <= ?
                  AND (tr.topic_id IS NULL OR tr.last_read_at < t.last_post_at)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, viewerTrustLevel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count unread topics", e);
        }
    }

    public Map<Long, Integer> countUnreadTopicsByCategory(Long userId, int viewerTrustLevel) {
        Map<Long, Integer> unreadCounts = new LinkedHashMap<>();
        if (userId == null) {
            return unreadCounts;
        }
        String sql = """
                SELECT t.category_id, COUNT(*) AS unread_count
                FROM topics t
                JOIN categories c ON c.id = t.category_id
                LEFT JOIN topic_reads tr ON tr.topic_id = t.id AND tr.user_id = ?
                WHERE t.status <> 'DELETED'
                  AND c.min_trust_level <= ?
                  AND (tr.topic_id IS NULL OR tr.last_read_at < t.last_post_at)
                GROUP BY t.category_id
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, viewerTrustLevel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    unreadCounts.put(rs.getLong("category_id"), rs.getInt("unread_count"));
                }
            }
            return unreadCounts;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count unread topics by category", e);
        }
    }

    private Query buildQuery(String mode, SearchCriteria criteria, Long authorId, Long currentUserId, boolean countOnly) {
        long viewerId = currentUserId == null ? 0L : currentUserId;
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();

        if (countOnly) {
            sql.append("SELECT COUNT(DISTINCT t.id) ");
        } else {
            sql.append("""
                    SELECT t.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                           c.name AS category_name, c.slug AS category_slug, c.color AS category_color, c.icon AS category_icon,
                           CASE WHEN lr.id IS NULL THEN FALSE ELSE TRUE END AS liked_by_current_user,
                           CASE WHEN bm.id IS NULL THEN FALSE ELSE TRUE END AS bookmarked_by_current_user,
                           ((t.likes_count * 5) + (t.replies_count * 4) + (t.bookmarks_count * 6) + (LOG10(t.views_count + 1) * 8)) /
                               POW(TIMESTAMPDIFF(HOUR, t.created_at, NOW()) + 2, 0.8) AS hot_score,
                           CASE WHEN ? = 0 THEN FALSE WHEN tr_card.topic_id IS NULL OR tr_card.last_read_at < t.last_post_at THEN TRUE ELSE FALSE END AS unread_by_current_user,
                           CASE WHEN ? = 0 THEN 0 ELSE GREATEST(0, t.replies_count + 1 - COALESCE(tr_card.last_read_post_number, 0)) END AS unread_posts_count
                    """);
            params.add(viewerId);
            params.add(viewerId);
        }

        sql.append("""
                FROM topics t
                JOIN users u ON u.id = t.user_id
                JOIN categories c ON c.id = t.category_id
                """);
        if (criteria.getTagId() != null) {
            sql.append(" JOIN topic_tags filter_tt ON filter_tt.topic_id = t.id AND filter_tt.tag_id = ? ");
            params.add(criteria.getTagId());
        }
        if ("bookmarks".equals(mode)) {
            sql.append(" JOIN bookmarks filter_bm ON filter_bm.target_type = 'TOPIC' AND filter_bm.target_id = t.id AND filter_bm.user_id = ? ");
            params.add(viewerId);
        }
        if ("unread".equals(mode)) {
            sql.append(" LEFT JOIN topic_reads tr_filter ON tr_filter.topic_id = t.id AND tr_filter.user_id = ? ");
            params.add(viewerId);
        }
        if (criteria.isHideReadTopics() && viewerId != 0L && !"unread".equals(mode)) {
            sql.append(" LEFT JOIN topic_reads tr_pref ON tr_pref.topic_id = t.id AND tr_pref.user_id = ? ");
            params.add(viewerId);
        }
        if (!countOnly) {
            sql.append(" LEFT JOIN reactions lr ON lr.target_type = 'TOPIC' AND lr.target_id = t.id AND lr.user_id = ? ");
            sql.append(" LEFT JOIN bookmarks bm ON bm.target_type = 'TOPIC' AND bm.target_id = t.id AND bm.user_id = ? ");
            sql.append(" LEFT JOIN topic_reads tr_card ON tr_card.topic_id = t.id AND tr_card.user_id = ? ");
            params.add(viewerId);
            params.add(viewerId);
            params.add(viewerId);
        }

        sql.append(" WHERE t.status <> 'DELETED' AND c.min_trust_level <= ? ");
        params.add(criteria.getViewerTrustLevel());
        if (criteria.getCategoryId() != null) {
            sql.append(" AND t.category_id = ? ");
            params.add(criteria.getCategoryId());
        }
        if (authorId != null) {
            sql.append(" AND t.user_id = ? ");
            params.add(authorId);
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            sql.append(" AND (t.title LIKE ? OR t.excerpt LIKE ?) ");
            String keyword = "%" + criteria.getKeyword().trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }
        if (criteria.getAuthor() != null && !criteria.getAuthor().isBlank()) {
            sql.append(" AND (u.username LIKE ? OR u.display_name LIKE ?) ");
            String author = "%" + criteria.getAuthor().trim() + "%";
            params.add(author);
            params.add(author);
        }
        if (criteria.isSolvedOnly()) {
            sql.append(" AND t.solved = TRUE ");
        }
        if (criteria.getCreatedWithinDays() != null && criteria.getCreatedWithinDays() > 0) {
            sql.append(" AND t.created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ");
            params.add(criteria.getCreatedWithinDays());
        }
        if (criteria.getMinReplies() != null) {
            sql.append(" AND t.replies_count >= ? ");
            params.add(criteria.getMinReplies());
        }
        if (criteria.getMaxReplies() != null) {
            sql.append(" AND t.replies_count <= ? ");
            params.add(criteria.getMaxReplies());
        }
        if (criteria.getMinViews() != null) {
            sql.append(" AND t.views_count >= ? ");
            params.add(criteria.getMinViews());
        }
        if (criteria.getMaxViews() != null) {
            sql.append(" AND t.views_count <= ? ");
            params.add(criteria.getMaxViews());
        }
        if ("unread".equals(mode)) {
            if (viewerId == 0L) {
                sql.append(" AND 1 = 0 ");
            } else {
                sql.append(" AND (tr_filter.topic_id IS NULL OR tr_filter.last_read_at < t.last_post_at) ");
            }
        }
        if (criteria.isHideReadTopics() && viewerId != 0L && !"unread".equals(mode)) {
            sql.append(" AND (tr_pref.topic_id IS NULL OR tr_pref.last_read_at < t.last_post_at) ");
        }

        if (!countOnly) {
            sql.append(orderBy(mode, criteria));
            sql.append(" LIMIT ? OFFSET ? ");
        }
        return new Query(sql.toString(), params);
    }

    private String orderBy(String mode, SearchCriteria criteria) {
        String orderBy = criteria.getOrderBy() == null ? "" : criteria.getOrderBy().trim().toUpperCase();
        if (!orderBy.isBlank()) {
            return switch (orderBy) {
                case "TOP" -> " ORDER BY hot_score DESC, t.last_post_at DESC ";
                case "VIEWS" -> " ORDER BY t.views_count DESC, t.last_post_at DESC ";
                case "REPLIES" -> " ORDER BY t.replies_count DESC, t.last_post_at DESC ";
                case "NEW" -> " ORDER BY t.created_at DESC ";
                default -> modeOrder(mode);
            };
        }
        return modeOrder(mode);
    }

    private String modeOrder(String mode) {
        return switch (mode == null ? "latest" : mode) {
            case "top" -> " ORDER BY hot_score DESC, t.last_post_at DESC ";
            case "new" -> " ORDER BY t.created_at DESC ";
            default -> " ORDER BY t.pinned DESC, t.last_post_at DESC ";
        };
    }

    private int bind(PreparedStatement ps, List<Object> params) throws Exception {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Long value) {
                ps.setLong(index++, value);
            } else if (param instanceof Integer value) {
                ps.setInt(index++, value);
            } else {
                ps.setString(index++, String.valueOf(param));
            }
        }
        return index;
    }

    private void attachTags(List<TopicCard> topics) {
        List<Long> ids = topics.stream().map(TopicCard::getId).toList();
        var tags = tagDao.findByTopicIds(ids);
        for (TopicCard topic : topics) {
            topic.setTags(tags.getOrDefault(topic.getId(), List.of()));
        }
    }

    private TopicCard mapCard(ResultSet rs) throws Exception {
        TopicCard topic = new TopicCard();
        topic.setId(rs.getLong("id"));
        topic.setUserId(rs.getLong("user_id"));
        topic.setCategoryId(rs.getLong("category_id"));
        topic.setTitle(rs.getString("title"));
        topic.setSlug(rs.getString("slug"));
        topic.setExcerpt(rs.getString("excerpt"));
        topic.setViewsCount(rs.getInt("views_count"));
        topic.setRepliesCount(rs.getInt("replies_count"));
        topic.setLikesCount(rs.getInt("likes_count"));
        topic.setBookmarksCount(rs.getInt("bookmarks_count"));
        topic.setPinned(rs.getBoolean("pinned"));
        topic.setSolved(rs.getBoolean("solved"));
        topic.setSolutionPostId(DaoMapper.nullableLong(rs, "solution_post_id"));
        topic.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        topic.setLastPostAt(DaoMapper.localDateTime(rs, "last_post_at"));
        topic.setHotScore(rs.getDouble("hot_score"));
        topic.setAuthorUsername(rs.getString("author_username"));
        topic.setAuthorDisplayName(rs.getString("author_display_name"));
        topic.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
        topic.setCategoryName(rs.getString("category_name"));
        topic.setCategorySlug(rs.getString("category_slug"));
        topic.setCategoryColor(rs.getString("category_color"));
        topic.setCategoryIcon(rs.getString("category_icon"));
        topic.setLikedByCurrentUser(rs.getBoolean("liked_by_current_user"));
        topic.setBookmarkedByCurrentUser(rs.getBoolean("bookmarked_by_current_user"));
        topic.setUnreadByCurrentUser(rs.getBoolean("unread_by_current_user"));
        topic.setUnreadPostsCount(rs.getInt("unread_posts_count"));
        return topic;
    }

    private record Query(String sql, List<Object> params) {
    }
}
