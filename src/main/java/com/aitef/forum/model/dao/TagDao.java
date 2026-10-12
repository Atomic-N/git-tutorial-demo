package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Tag;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.SlugUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TagDao {
    private static final String TAG_SELECT = """
            SELECT t.id, t.name, t.slug,
                   COALESCE(tg.name, t.group_name) AS group_name,
                   t.color, t.icon, t.topic_count
            FROM tags t
            LEFT JOIN tag_groups tg ON tg.id = t.tag_group_id
            """;

    public List<Tag> findAll() {
        String sql = TAG_SELECT + " ORDER BY COALESCE(tg.sort_order, 9999) ASC, COALESCE(tg.name, t.group_name) ASC, t.topic_count DESC, t.name ASC";
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tags.add(map(rs));
            }
            return tags;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tags", e);
        }
    }

    public Optional<Tag> findBySlug(String slug) {
        String sql = TAG_SELECT + " WHERE t.slug = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tag by slug", e);
        }
    }

    public Map<String, List<Tag>> groupByName() {
        Map<String, List<Tag>> grouped = new LinkedHashMap<>();
        for (Tag tag : findAll()) {
            grouped.computeIfAbsent(tag.getGroupName(), ignored -> new ArrayList<>()).add(tag);
        }
        return grouped;
    }

    public Map<Long, List<Tag>> findByTopicIds(List<Long> topicIds) {
        Map<Long, List<Tag>> result = new HashMap<>();
        if (topicIds == null || topicIds.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", topicIds.stream().map(id -> "?").toList());
        String sql = """
                SELECT tt.topic_id, t.id, t.name, t.slug,
                       COALESCE(tgg.name, t.group_name) AS group_name,
                       t.color, t.icon, t.topic_count
                FROM topic_tags tt
                JOIN tags t ON t.id = tt.tag_id
                LEFT JOIN tag_groups tgg ON tgg.id = t.tag_group_id
                WHERE tt.topic_id IN (%s)
                ORDER BY t.topic_count DESC, t.name ASC
                """.formatted(placeholders);
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < topicIds.size(); i++) {
                ps.setLong(i + 1, topicIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.computeIfAbsent(rs.getLong("topic_id"), ignored -> new ArrayList<>()).add(map(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tags by topic ids", e);
        }
    }

    public List<Tag> findByTopicId(Long topicId) {
        String sql = """
                SELECT t.id, t.name, t.slug,
                       COALESCE(tgg.name, t.group_name) AS group_name,
                       t.color, t.icon, t.topic_count
                FROM topic_tags tt
                JOIN tags t ON t.id = tt.tag_id
                LEFT JOIN tag_groups tgg ON tgg.id = t.tag_group_id
                WHERE tt.topic_id = ?
                ORDER BY t.topic_count DESC, t.name ASC
                """;
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(map(rs));
                }
            }
            return tags;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load topic tags", e);
        }
    }

    public List<Long> findTagIdsByTopicId(Connection conn, Long topicId) {
        String sql = "SELECT tag_id FROM topic_tags WHERE topic_id = ?";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("tag_id"));
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load topic tag ids", e);
        }
    }

    public Long findOrCreate(Connection conn, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isBlank()) {
            return null;
        }
        String slug = SlugUtil.fromTitle(name).toLowerCase();
        String findSql = "SELECT id FROM tags WHERE name = ? OR slug = ?";
        try (PreparedStatement find = conn.prepareStatement(findSql)) {
            find.setString(1, name);
            find.setString(2, slug);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find tag", e);
        }

        String insertSql = """
                INSERT INTO tags (name, slug, tag_group_id, group_name)
                VALUES (?, ?, (SELECT id FROM tag_groups WHERE name = '常用标签' LIMIT 1), '常用标签')
                """;
        try (PreparedStatement insert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, name);
            insert.setString(2, slug);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for tag");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create tag", e);
        }
    }

    public void attachTag(Connection conn, Long topicId, Long tagId) {
        if (tagId == null) {
            return;
        }
        String sql = "INSERT IGNORE INTO topic_tags (topic_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to attach tag", e);
        }
    }

    public void clearTopicTags(Connection conn, Long topicId) {
        String sql = "DELETE FROM topic_tags WHERE topic_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clear topic tags", e);
        }
    }

    public void refreshTopicCount(Connection conn, Long tagId) {
        if (tagId == null) {
            return;
        }
        String sql = "UPDATE tags SET topic_count = (SELECT COUNT(*) FROM topic_tags WHERE tag_id = ?) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tagId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh tag count", e);
        }
    }

    private Tag map(ResultSet rs) throws Exception {
        Tag tag = new Tag();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        tag.setSlug(rs.getString("slug"));
        tag.setGroupName(rs.getString("group_name"));
        tag.setColor(rs.getString("color"));
        tag.setIcon(rs.getString("icon"));
        tag.setTopicCount(rs.getInt("topic_count"));
        return tag;
    }
}
