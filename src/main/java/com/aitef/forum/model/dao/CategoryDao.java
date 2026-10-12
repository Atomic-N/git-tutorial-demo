package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Category;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY position ASC, id ASC";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(map(rs));
            }
            return categories;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load categories", e);
        }
    }

    public List<Category> findVisible(int trustLevel) {
        String sql = "SELECT * FROM categories WHERE min_trust_level <= ? ORDER BY position ASC, id ASC";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trustLevel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(map(rs));
                }
            }
            return categories;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load visible categories", e);
        }
    }

    public Optional<Category> findById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load category by id", e);
        }
    }

    public Optional<Category> findBySlug(String slug) {
        String sql = "SELECT * FROM categories WHERE slug = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load category by slug", e);
        }
    }

    private Category map(ResultSet rs) throws Exception {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setParentId(DaoMapper.nullableLong(rs, "parent_id"));
        category.setName(rs.getString("name"));
        category.setSlug(rs.getString("slug"));
        category.setDescription(rs.getString("description"));
        category.setColor(rs.getString("color"));
        category.setTextColor(rs.getString("text_color"));
        category.setIcon(rs.getString("icon"));
        category.setPosition(rs.getInt("position"));
        category.setMinTrustLevel(rs.getInt("min_trust_level"));
        category.setTopicCount(rs.getInt("topic_count"));
        category.setPostCount(rs.getInt("post_count"));
        return category;
    }
}
