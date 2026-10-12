package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.Badge;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.BadgeService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/admin/badges", "/admin/badges/*"})
public class AdminBadgeServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final BadgeService badgeService = new BadgeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        adminService.requireAdmin(currentUser(request));
        request.setAttribute("badges", badgeService.listForAdmin());
        request.setAttribute("pageTitle", "徽章管理");
        request.setAttribute("activeNav", "admin");
        render(request, response, "admin/badges.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        adminService.requireAdmin(currentUser(request));
        String action = pathAfter(request, "/admin/badges");
        try {
            switch (action) {
                case "create" -> {
                    badgeService.createBadge(currentUserId(request), badgeFromRequest(request, false));
                    redirect(response, request, "message", "徽章已创建", "#badge-form");
                }
                case "update" -> {
                    badgeService.updateBadge(currentUserId(request), badgeFromRequest(request, true));
                    redirect(response, request, "message", "徽章已保存", "#badge-list");
                }
                case "delete" -> {
                    badgeService.deleteBadge(currentUserId(request), longParam(request, "badgeId"));
                    redirect(response, request, "message", "徽章已删除", "#badge-list");
                }
                case "award" -> {
                    badgeService.awardBadge(currentUserId(request), longParam(request, "badgeId"), stringParam(request, "username"));
                    redirect(response, request, "message", "徽章已授予", "#badge-list");
                }
                case "revoke" -> {
                    badgeService.revokeBadge(currentUserId(request), longParam(request, "badgeId"), stringParam(request, "username"));
                    redirect(response, request, "message", "徽章已撤回", "#badge-list");
                }
                default -> throw new ValidationException("未知徽章操作");
            }
        } catch (ValidationException e) {
            redirect(response, request, "error", e.getMessage(), "create".equals(action) ? "#badge-form" : "#badge-list");
        }
    }

    private Badge badgeFromRequest(HttpServletRequest request, boolean includeId) {
        Badge badge = new Badge();
        if (includeId) {
            badge.setId(longParam(request, "badgeId"));
        }
        badge.setCode(stringParam(request, "code"));
        badge.setName(stringParam(request, "name"));
        badge.setDescription(stringParam(request, "description"));
        badge.setIcon(stringParam(request, "icon"));
        badge.setColor(stringParam(request, "color"));
        badge.setSortOrder(intParam(request, "sortOrder", 0));
        badge.setRuleType(stringParam(request, "ruleType"));
        badge.setRuleThreshold(intParam(request, "ruleThreshold", 0));
        return badge;
    }

    private void redirect(HttpServletResponse response, HttpServletRequest request,
                          String key, String message, String fragment) throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/badges?"
                + key + "=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                + (fragment == null ? "" : fragment));
    }
}
