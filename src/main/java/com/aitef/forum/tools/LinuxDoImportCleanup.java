package com.aitef.forum.tools;

import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class LinuxDoImportCleanup {
    private static final String SOURCE_PREFIX = "SOURCE_LINK::https://linux.do/";

    private LinuxDoImportCleanup() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CleanupStats stats = cleanup(conn);
                refreshAllCounts(conn);
                conn.commit();
                System.out.println("deleted_topics=" + stats.deletedTopics());
                System.out.println("deleted_posts=" + stats.deletedPosts());
                System.out.println("deleted_users=" + stats.deletedUsers());
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static CleanupStats cleanup(Connection conn) throws Exception {
        List<Long> topicIds = findImportedTopicIds(conn);
        if (topicIds.isEmpty()) {
            return new CleanupStats(0, 0, 0);
        }

        String topicPlaceholders = placeholders(topicIds.size());
        List<Long> postIds = findPostIds(conn, topicIds, topicPlaceholders);

        deleteByIds(conn, "DELETE FROM reactions WHERE target_type = 'TOPIC' AND target_id IN (" + topicPlaceholders + ")", topicIds);
        deleteByIds(conn, "DELETE FROM bookmarks WHERE target_type = 'TOPIC' AND target_id IN (" + topicPlaceholders + ")", topicIds);
        deleteByIds(conn, "DELETE FROM notifications WHERE target_type = 'TOPIC' AND target_id IN (" + topicPlaceholders + ")", topicIds);
        deleteByIds(conn, "DELETE FROM reports WHERE target_type = 'TOPIC' AND target_id IN (" + topicPlaceholders + ")", topicIds);

        if (!postIds.isEmpty()) {
            String postPlaceholders = placeholders(postIds.size());
            deleteByIds(conn, "DELETE FROM reactions WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", postIds);
            deleteByIds(conn, "DELETE FROM bookmarks WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", postIds);
            deleteByIds(conn, "DELETE FROM notifications WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", postIds);
            deleteByIds(conn, "DELETE FROM reports WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", postIds);
        }

        int deletedTopics = deleteByIds(conn, "DELETE FROM topics WHERE id IN (" + topicPlaceholders + ")", topicIds);
        int deletedUsers = deleteImportedOrphans(conn);
        return new CleanupStats(deletedTopics, postIds.size(), deletedUsers);
    }

    private static List<Long> findImportedTopicIds(Connection conn) throws Exception {
        List<Long> topicIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT t.id
                FROM topics t
                JOIN posts p ON p.topic_id = t.id AND p.post_number = 1
                WHERE p.raw_content LIKE ?
                ORDER BY t.id
                """)) {
            ps.setString(1, "%" + SOURCE_PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topicIds.add(rs.getLong(1));
                }
            }
        }
        return topicIds;
    }

    private static List<Long> findPostIds(Connection conn, List<Long> topicIds, String topicPlaceholders) throws Exception {
        List<Long> postIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM posts WHERE topic_id IN (" + topicPlaceholders + ")")) {
            bindIds(ps, topicIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    postIds.add(rs.getLong(1));
                }
            }
        }
        return postIds;
    }

    private static int deleteImportedOrphans(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM users
                WHERE username LIKE ?
                  AND NOT EXISTS (SELECT 1 FROM topics t WHERE t.user_id = users.id)
                  AND NOT EXISTS (SELECT 1 FROM posts p WHERE p.user_id = users.id)
                """)) {
            ps.setString(1, "linuxdo\\_%");
            return ps.executeUpdate();
        }
    }

    private static int deleteByIds(Connection conn, String sql, List<Long> ids) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids);
            return ps.executeUpdate();
        }
    }

    private static void refreshAllCounts(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE users u
                SET topics_count = (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'),
                    posts_count = (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'),
                    likes_received_count = (
                        SELECT COUNT(*)
                        FROM reactions r
                        WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'))
                           OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'))
                    )
                """)) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE categories c
                SET topic_count = (SELECT COUNT(*) FROM topics t WHERE t.category_id = c.id AND t.status <> 'DELETED'),
                    post_count = (
                        SELECT COUNT(*)
                        FROM posts p
                        JOIN topics t ON t.id = p.topic_id
                        WHERE t.category_id = c.id AND t.status <> 'DELETED' AND p.status <> 'DELETED'
                    )
                """)) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE tags
                SET topic_count = (SELECT COUNT(*) FROM topic_tags tt WHERE tt.tag_id = tags.id)
                """)) {
            ps.executeUpdate();
        }
    }

    private static String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private static void bindIds(PreparedStatement ps, List<Long> ids) throws Exception {
        for (int i = 0; i < ids.size(); i++) {
            ps.setLong(i + 1, ids.get(i));
        }
    }

    private record CleanupStats(int deletedTopics, int deletedPosts, int deletedUsers) {
    }
}
