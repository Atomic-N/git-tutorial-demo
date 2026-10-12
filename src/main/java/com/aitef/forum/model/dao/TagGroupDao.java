package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.TagGroup;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagGroupDao {
    public List<TagGroup> findAll() {
        String sql = "SELECT * FROM tag_groups ORDER BY sort_order ASC, id ASC";
        List<TagGroup> groups = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(map(rs));
            }
            return groups;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load tag groups", e);
        }
    }

    public Optional<TagGroup> findById(Long id) {
        String sql = "SELECT * FROM tag_groups WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find tag group", e);
        }
    }

    private TagGroup map(ResultSet rs) throws Exception {
        TagGroup group = new TagGroup();
        group.setId(rs.getLong("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setColor(rs.getString("color"));
        group.setSortOrder(rs.getInt("sort_order"));
        group.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        return group;
    }
}
