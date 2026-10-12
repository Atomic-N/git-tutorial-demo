package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.Draft;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class DraftDao {
    public Optional<Draft> find(Long userId, String draftType, Long targetId) {
        String sql = """
                SELECT * FROM drafts
                WHERE user_id = ? AND draft_type = ? AND ((target_id IS NULL AND ? IS NULL) OR target_id = ?)
                ORDER BY updated_at DESC
                LIMIT 1
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, draftType);
            if (targetId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, targetId);
                ps.setLong(4, targetId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find draft", e);
        }
    }

    public Long save(Long userId, String draftType, Long targetId, String title, String content) {
        String lookupSql = "SELECT id FROM drafts WHERE user_id = ? AND draft_type = ? AND ((target_id IS NULL AND ? IS NULL) OR target_id = ?) LIMIT 1";
        try (Connection conn = Database.getConnection()) {
            Long existingId = null;
            try (PreparedStatement lookup = conn.prepareStatement(lookupSql)) {
                lookup.setLong(1, userId);
                lookup.setString(2, draftType);
                if (targetId == null) {
                    lookup.setNull(3, java.sql.Types.BIGINT);
                    lookup.setNull(4, java.sql.Types.BIGINT);
                } else {
                    lookup.setLong(3, targetId);
                    lookup.setLong(4, targetId);
                }
                try (ResultSet rs = lookup.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getLong("id");
                    }
                }
            }
            if (existingId != null) {
                update(conn, existingId, title, content);
                return existingId;
            }
            return insert(conn, userId, draftType, targetId, title, content);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save draft", e);
        }
    }

    public void delete(Long userId, String draftType, Long targetId) {
        String sql = "DELETE FROM drafts WHERE user_id = ? AND draft_type = ? AND ((target_id IS NULL AND ? IS NULL) OR target_id = ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, draftType);
            if (targetId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, targetId);
                ps.setLong(4, targetId);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete draft", e);
        }
    }

    private Long insert(Connection conn, Long userId, String draftType, Long targetId, String title, String content) throws Exception {
        String sql = """
                INSERT INTO drafts (user_id, draft_type, target_id, title, content)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, draftType);
            if (targetId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, targetId);
            }
            ps.setString(4, title);
            ps.setString(5, content);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for draft");
        }
    }

    private void update(Connection conn, Long draftId, String title, String content) throws Exception {
        String sql = "UPDATE drafts SET title = ?, content = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, draftId);
            ps.executeUpdate();
        }
    }

    private Draft map(ResultSet rs) throws Exception {
        Draft draft = new Draft();
        draft.setId(rs.getLong("id"));
        draft.setUserId(rs.getLong("user_id"));
        draft.setDraftType(rs.getString("draft_type"));
        draft.setTargetId(DaoMapper.nullableLong(rs, "target_id"));
        draft.setTitle(rs.getString("title"));
        draft.setContent(rs.getString("content"));
        draft.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        return draft;
    }
}