package com.aitef.forum.model.dao;

import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.entity.Report;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportDao {
    public Long create(Long reporterId, String targetType, Long targetId, String reason, String detail) {
        String sql = """
                INSERT INTO reports (reporter_id, target_type, target_id, reason, detail)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reporterId);
            ps.setString(2, targetType);
            ps.setLong(3, targetId);
            ps.setString(4, reason);
            ps.setString(5, detail);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for report");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create report", e);
        }
    }

    public Page<Report> list(String status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(5, size));
        List<Object> params = new ArrayList<>();
        String where = "";
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            where = " WHERE r.status = ? ";
            params.add(status);
        }
        String countSql = "SELECT COUNT(*) FROM reports r " + where;
        String listSql = """
                SELECT r.*, u.display_name AS reporter_name,
                       CASE
                           WHEN r.target_type = 'TOPIC' THEN (SELECT title FROM topics WHERE id = r.target_id)
                           WHEN r.target_type = 'POST' THEN (SELECT CONCAT('回复 #', post_number, ' / ', (SELECT title FROM topics WHERE id = posts.topic_id)) FROM posts WHERE id = r.target_id)
                           ELSE CONCAT(r.target_type, ' #', r.target_id)
                       END AS target_title,
                       CASE
                           WHEN r.target_type = 'TOPIC' THEN CONCAT('/t/', r.target_id)
                           WHEN r.target_type = 'POST' THEN (SELECT CONCAT('/t/', topic_id, '#post-', id) FROM posts WHERE id = r.target_id)
                           ELSE '/latest'
                       END AS target_url
                FROM reports r
                JOIN users u ON u.id = r.reporter_id
                %s
                ORDER BY CASE r.status WHEN 'PENDING' THEN 0 ELSE 1 END, r.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(where);
        try (Connection conn = Database.getConnection()) {
            long total;
            try (PreparedStatement count = conn.prepareStatement(countSql)) {
                bind(count, params);
                try (ResultSet rs = count.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }
            List<Report> reports = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(listSql)) {
                int index = bind(ps, params);
                ps.setInt(index++, safeSize);
                ps.setInt(index, (safePage - 1) * safeSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reports.add(map(rs));
                    }
                }
            }
            return new Page<>(reports, safePage, safeSize, total);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list reports", e);
        }
    }

    public Optional<Report> findById(Long reportId) {
        String sql = """
                SELECT r.*, u.display_name AS reporter_name,
                       CASE
                           WHEN r.target_type = 'TOPIC' THEN (SELECT title FROM topics WHERE id = r.target_id)
                           WHEN r.target_type = 'POST' THEN (SELECT CONCAT('回复 #', post_number, ' / ', (SELECT title FROM topics WHERE id = posts.topic_id)) FROM posts WHERE id = r.target_id)
                           ELSE CONCAT(r.target_type, ' #', r.target_id)
                       END AS target_title,
                       CASE
                           WHEN r.target_type = 'TOPIC' THEN CONCAT('/t/', r.target_id)
                           WHEN r.target_type = 'POST' THEN (SELECT CONCAT('/t/', topic_id, '#post-', id) FROM posts WHERE id = r.target_id)
                           ELSE '/latest'
                       END AS target_url
                FROM reports r
                JOIN users u ON u.id = r.reporter_id
                WHERE r.id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find report", e);
        }
    }

    public void updateStatus(Connection conn, Long reportId, String status) {
        String sql = "UPDATE reports SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, reportId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update report status", e);
        }
    }

    public void updateStatus(Long reportId, String status) {
        String sql = "UPDATE reports SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, reportId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update report status", e);
        }
    }

    private int bind(PreparedStatement ps, List<Object> params) throws Exception {
        int index = 1;
        for (Object param : params) {
            ps.setObject(index++, param);
        }
        return index;
    }

    private Report map(ResultSet rs) throws Exception {
        Report report = new Report();
        report.setId(rs.getLong("id"));
        report.setReporterId(rs.getLong("reporter_id"));
        report.setReporterName(rs.getString("reporter_name"));
        report.setTargetType(rs.getString("target_type"));
        report.setTargetId(rs.getLong("target_id"));
        report.setTargetTitle(rs.getString("target_title"));
        report.setTargetUrl(rs.getString("target_url"));
        report.setReason(rs.getString("reason"));
        report.setDetail(rs.getString("detail"));
        report.setStatus(rs.getString("status"));
        report.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        report.setUpdatedAt(DaoMapper.localDateTime(rs, "updated_at"));
        return report;
    }
}
