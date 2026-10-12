package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.RoleService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/content/*")
public class AdminContentServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final RoleService roleService = new RoleService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        adminService.requireAdmin(currentUser(request));
        String action = pathAfter(request, "/admin/content");
        try {
            if ("delete-topic".equals(action)) {
                adminService.deleteTopic(currentUserId(request), longParam(request, "topicId"));
                redirectBack(request, response, "message", "话题已删除");
                return;
            }
            if ("delete-post".equals(action)) {
                Long topicId = adminService.deletePost(currentUserId(request), longParam(request, "postId"));
                response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?message=" + URLEncoder.encode("回复已删除", StandardCharsets.UTF_8));
                return;
            }
            if ("pin-topic".equals(action)) {
                adminService.pinTopic(currentUserId(request), longParam(request, "topicId"), true);
                redirectBack(request, response, "message", "话题已置顶");
                return;
            }
            if ("unpin-topic".equals(action)) {
                adminService.pinTopic(currentUserId(request), longParam(request, "topicId"), false);
                redirectBack(request, response, "message", "话题已取消置顶");
                return;
            }
            if ("bulk-topics".equals(action)) {
                String bulkAction = stringParam(request, "bulkAction");
                int count = adminService.bulkUpdateTopics(currentUserId(request), longValues(request, "topicIds"), bulkAction);
                redirectBack(request, response, "message", bulkTopicMessage(bulkAction, count));
                return;
            }
            if ("update-user".equals(action)) {
                adminService.requireAdmin(currentUser(request));
                Long userId = longParam(request, "userId");
                adminService.moderateUser(currentUserId(request), userId, stringParam(request, "status"), intParam(request, "trustLevel", 0));
                roleService.updateUserRoles(currentUserId(request), userId, stringValues(request, "roles"), longValues(request, "moderatedCategories"));
                redirectBack(request, response, "message", "用户状态已更新");
                return;
            }
            if ("refresh-badges".equals(action)) {
                adminService.requireAdmin(currentUser(request));
                int count = adminService.refreshBadges();
                redirectBack(request, response, "message", "已刷新 " + count + " 位用户的徽章");
                return;
            }
            throw new ValidationException("未知管理操作");
        } catch (ValidationException e) {
            redirectBack(request, response, "error", e.getMessage());
        }
    }

    private List<String> stringValues(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? List.of() : List.of(values);
    }

    private List<Long> longValues(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String value : values) {
            try {
                if (value != null && !value.isBlank()) {
                    result.add(Long.parseLong(value));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private String bulkTopicMessage(String action, int count) {
        String normalized = action == null ? "" : action.trim().toLowerCase();
        return switch (normalized) {
            case "pin" -> "已置顶 " + count + " 个话题";
            case "unpin" -> "已取消置顶 " + count + " 个话题";
            case "delete" -> "已删除 " + count + " 个话题";
            default -> "已处理 " + count + " 个话题";
        };
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String key, String message) throws IOException {
        String target = safeReturnTo(request);
        String fragment = "";
        int fragmentIndex = target.indexOf('#');
        if (fragmentIndex >= 0) {
            fragment = target.substring(fragmentIndex);
            target = target.substring(0, fragmentIndex);
        }
        String separator = target.contains("?") ? "&" : "?";
        response.sendRedirect(target + separator + key + "=" + URLEncoder.encode(message, StandardCharsets.UTF_8) + fragment);
    }

    private String safeReturnTo(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String fallback = contextPath + "/admin/dashboard";
        String returnTo = request.getParameter("returnTo");
        if (returnTo == null || returnTo.isBlank()) {
            return fallback;
        }
        returnTo = returnTo.trim();
        if (returnTo.startsWith(contextPath + "/admin")) {
            return returnTo;
        }
        if (returnTo.startsWith("/admin")) {
            return contextPath + returnTo;
        }
        return fallback;
    }
}
