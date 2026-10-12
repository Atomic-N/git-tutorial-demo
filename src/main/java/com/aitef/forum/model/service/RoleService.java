package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.RoleDao;
import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.Role;
import com.aitef.forum.util.Database;
import com.aitef.forum.util.ValidationException;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RoleService {
    private final RoleDao roleDao = new RoleDao();
    private final UserDao userDao = new UserDao();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Role> listRoles() {
        return roleDao.findAll();
    }

    public void updateUserRoles(Long actorId, Long userId, List<String> codes) {
        updateUserRoles(actorId, userId, codes, List.of());
    }

    public void updateUserRoles(Long actorId, Long userId, List<String> codes, List<Long> moderatedCategoryIds) {
        if (userId == null) {
            throw new ValidationException("用户不存在");
        }
        List<String> normalized = (codes == null ? List.<String>of() : codes).stream()
                .map(code -> code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
        List<Long> normalizedCategoryIds = (moderatedCategoryIds == null ? List.<Long>of() : moderatedCategoryIds).stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        roleDao.ensureModeratorScopeTable();
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                userDao.findById(conn, userId).orElseThrow(() -> new ValidationException("用户不存在"));
                roleDao.replaceUserRoles(conn, userId, normalized);
                List<Long> effectiveCategoryIds = normalized.contains("MODERATOR") ? normalizedCategoryIds : List.of();
                roleDao.replaceModeratedCategories(conn, userId, effectiveCategoryIds);
                String primaryRole = normalized.contains("ADMIN") ? "ADMIN" : normalized.contains("MODERATOR") ? "MODERATOR" : "USER";
                userDao.updatePrimaryRole(conn, userId, primaryRole);
                auditLogService.log(conn, actorId, "USER_ROLE_UPDATE", "USER", userId,
                        "roles=" + String.join(",", normalized) + ",moderatedCategories=" + effectiveCategoryIds);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update user roles", e);
        }
    }
}
