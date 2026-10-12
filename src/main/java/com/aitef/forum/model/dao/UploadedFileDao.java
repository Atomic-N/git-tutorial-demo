package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.UploadedFile;
import com.aitef.forum.util.DaoMapper;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UploadedFileDao {
    public Long create(UploadedFile file) {
        String sql = """
                INSERT INTO uploaded_files (user_id, original_name, stored_name, content_type, file_size, public_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, file.getUserId());
            ps.setString(2, file.getOriginalName());
            ps.setString(3, file.getStoredName());
            ps.setString(4, file.getContentType());
            ps.setLong(5, file.getFileSize());
            ps.setString(6, file.getPublicUrl());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("No generated key returned for uploaded file");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save uploaded file", e);
        }
    }

    public List<UploadedFile> listByUserId(Long userId, int limit) {
        String sql = """
                SELECT * FROM uploaded_files
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<UploadedFile> files = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, Math.min(limit, 50)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    files.add(map(rs));
                }
            }
            return files;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list uploaded files", e);
        }
    }

    private UploadedFile map(ResultSet rs) throws Exception {
        UploadedFile file = new UploadedFile();
        file.setId(rs.getLong("id"));
        file.setUserId(rs.getLong("user_id"));
        file.setOriginalName(rs.getString("original_name"));
        file.setStoredName(rs.getString("stored_name"));
        file.setContentType(rs.getString("content_type"));
        file.setFileSize(rs.getLong("file_size"));
        file.setPublicUrl(rs.getString("public_url"));
        file.setCreatedAt(DaoMapper.localDateTime(rs, "created_at"));
        return file;
    }
}