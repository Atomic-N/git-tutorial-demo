package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.AuditLogDao;
import com.aitef.forum.model.dto.Page;
import com.aitef.forum.model.entity.AuditLog;

import java.sql.Connection;

public class AuditLogService {
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public void log(Connection conn, Long actorId, String action, String targetType, Long targetId, String detail) {
        auditLogDao.create(conn, actorId, action, targetType, targetId, detail);
    }

    public void log(Long actorId, String action, String targetType, Long targetId, String detail) {
        try (Connection conn = com.aitef.forum.util.Database.getConnection()) {
            auditLogDao.create(conn, actorId, action, targetType, targetId, detail);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write audit log", e);
        }
    }

    public Page<AuditLog> list(int page, int size) {
        return auditLogDao.list(page, size);
    }
}
