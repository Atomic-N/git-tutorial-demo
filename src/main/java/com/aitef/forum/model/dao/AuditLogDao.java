package com.aitef.forum.model.dao;

import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.entity.AuditLog;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {
    public void create(Connection conn, Long actorId, String action, String targetType, Long targetId, String detail) {
        String sql = """
                INSERT INTO audit_logs (actor_id, action, target_type, target_id, detail)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (actorId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, actorId);
            }
            ps.setString(2, action);
            ps.setString(3, targetType);
            if (targetId == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, targetId);
            }
            ps.setString(5, detail == null ? "" : detail);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create audit log", e);
        }
    }

    public Page<AuditLog> list(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(5, size));
        String countSql = "SELECT COUNT(*) FROM audit_logs";
        String listSql = """
                SELECT a.*, u.display_name AS actor_name
                FROM audit_logs a
                LEFT JOIN users u ON u.id = a.actor_id
                ORDER BY a.created_at DESC
                LIMIT ? OFFSET ?
                """;
        List<AuditLog> items = new ArrayList<>();
        long total;
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement countPs = conn.prepareStatement(countSql);
                 ResultSet countRs = countPs.executeQuery()) {
                total = countRs.next() ? countRs.getLong(1) : 0L;
            }
            try (PreparedStatement ps = conn.prepareStatement(listSql)) {
                ps.setInt(1, safeSize);
                ps.setInt(2, (safePage - 1) * safeSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
            return new Page<>(items, safePage, safeSize, total);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list audit logs", e);
        }
    }

    private AuditLog map(ResultSet rs) throws Exception {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        log.setActorId(DaoMapper.nullableLong(rs, "actor_id"));
        log.setActorName(rs.getString("actor_name"));
        log.setAction(rs.getString("action"));
        log.setTargetType(rs.getString("target_type"));
        log.setTargetId(DaoMapper.nullableLong(rs, "target_id"));
        log.setDetail(rs.getString("detail"));
        log.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        return log;
    }
}
