package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Post;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostDao {
    public List<Post> findByTopicId(Long topicId, Long currentUserId) {
        String sql = """
                SELECT p.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                       u.role AS author_role,
                       ru.username AS reply_to_author_username, ru.display_name AS reply_to_author_display_name,
                       rp.post_number AS reply_to_post_number, rp.raw_content AS reply_to_raw_content,
                       CASE WHEN lr.id IS NULL THEN FALSE ELSE TRUE END AS liked_by_current_user,
                       CASE WHEN t.solution_post_id = p.id THEN TRUE ELSE FALSE END AS is_solution
                FROM posts p
                JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = p.user_id
                LEFT JOIN posts rp ON rp.id = p.reply_to_post_id AND rp.status <> 'DELETED'
                LEFT JOIN users ru ON ru.id = rp.user_id
                LEFT JOIN reactions lr ON lr.target_type = 'POST' AND lr.target_id = p.id AND lr.user_id = ?
                WHERE p.topic_id = ? AND p.status <> 'DELETED'
                ORDER BY p.post_number ASC
                """;
        long viewerId = currentUserId == null ? 0L : currentUserId;
        List<Post> posts = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, viewerId);
            ps.setLong(2, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(map(rs));
                }
            }
            return posts;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load posts", e);
        }
    }

    public Optional<Post> findById(Long id) {
        String sql = """
                SELECT p.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                       u.role AS author_role,
                       ru.username AS reply_to_author_username, ru.display_name AS reply_to_author_display_name,
                       rp.post_number AS reply_to_post_number, rp.raw_content AS reply_to_raw_content,
                       FALSE AS liked_by_current_user,
                       FALSE AS is_solution
                FROM posts p
                JOIN users u ON u.id = p.user_id
                LEFT JOIN posts rp ON rp.id = p.reply_to_post_id AND rp.status <> 'DELETED'
                LEFT JOIN users ru ON ru.id = rp.user_id
                WHERE p.id = ? AND p.status <> 'DELETED'
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load post", e);
        }
    }

    public Optional<Post> findFirstByTopicId(Long topicId) {
        String sql = """
                SELECT p.*, u.username AS author_username, u.display_name AS author_display_name, u.avatar_url AS author_avatar_url,
                       u.role AS author_role,
                       ru.username AS reply_to_author_username, ru.display_name AS reply_to_author_display_name,
                       rp.post_number AS reply_to_post_number, rp.raw_content AS reply_to_raw_content,
                       FALSE AS liked_by_current_user,
                       FALSE AS is_solution
                FROM posts p
                JOIN users u ON u.id = p.user_id
                LEFT JOIN posts rp ON rp.id = p.reply_to_post_id AND rp.status <> 'DELETED'
                LEFT JOIN users ru ON ru.id = rp.user_id
                WHERE p.topic_id = ? AND p.post_number = 1 AND p.status <> 'DELETED'
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load first post", e);
        }
    }

    public int nextPostNumber(Connection conn, Long topicId) {
        String sql = "SELECT COALESCE(MAX(post_number), 0) + 1 FROM posts WHERE topic_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate next post number", e);
        }
    }

    public Long create(Connection conn, Long topicId, Long userId, Long replyToPostId, int postNumber, String rawContent, String cookedContent) {
        String sql = """
                INSERT INTO posts (topic_id, user_id, reply_to_post_id, post_number, raw_content, cooked_content)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, topicId);
            ps.setLong(2, userId);
            if (replyToPostId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, replyToPostId);
            }
            ps.setInt(4, postNumber);
            ps.setString(5, rawContent);
            ps.setString(6, cookedContent);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for post");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create post", e);
        }
    }

    public int maxPostNumber(Long topicId) {
        String sql = "SELECT COALESCE(MAX(post_number), 0) FROM posts WHERE topic_id = ? AND status <> 'DELETED'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get max post number", e);
        }
    }

    public void softDelete(Connection conn, Long postId) {
        String sql = "UPDATE posts SET status = 'DELETED' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete post", e);
        }
    }

    public void updateContent(Connection conn, Long postId, String rawContent, String cookedContent) {
        String sql = """
                UPDATE posts
                SET raw_content = ?, cooked_content = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rawContent);
            ps.setString(2, cookedContent);
            ps.setLong(3, postId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update post", e);
        }
    }

    private Post map(ResultSet rs) throws Exception {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setTopicId(rs.getLong("topic_id"));
        post.setUserId(rs.getLong("user_id"));
        post.setReplyToPostId(DaoMapper.nullableLong(rs, "reply_to_post_id"));
        post.setPostNumber(rs.getInt("post_number"));
        post.setRawContent(rs.getString("raw_content"));
        post.setCookedContent(rs.getString("cooked_content"));
        post.setLikesCount(rs.getInt("likes_count"));
        post.setStatus(rs.getString("status"));
        post.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        post.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        post.setAuthorUsername(rs.getString("author_username"));
        post.setAuthorDisplayName(rs.getString("author_display_name"));
        post.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
        post.setAuthorRole(rs.getString("author_role"));
        post.setReplyToAuthorUsername(rs.getString("reply_to_author_username"));
        post.setReplyToAuthorDisplayName(rs.getString("reply_to_author_display_name"));
        int replyToPostNumber = rs.getInt("reply_to_post_number");
        post.setReplyToPostNumber(rs.wasNull() ? null : replyToPostNumber);
        post.setReplyToExcerpt(DaoMapper.safeExcerpt(rs.getString("reply_to_raw_content"), 72));
        post.setLikedByCurrentUser(rs.getBoolean("liked_by_current_user"));
        post.setSolution(rs.getBoolean("is_solution"));
        return post;
    }
}
