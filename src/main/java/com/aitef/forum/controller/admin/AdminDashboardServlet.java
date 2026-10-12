package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.RoleService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/admin", "/admin/dashboard"})
public class AdminDashboardServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final RoleService roleService = new RoleService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        adminService.requireAdmin(currentUser(request));
        SearchCriteria topicCriteria = topicCriteria(request);
        String userKeyword = stringParam(request, "userKeyword");
        String userStatus = normalizedSelect(request, "userStatus", "ALL");
        String userRole = normalizedSelect(request, "userRole", "ALL");
        Integer userTrustLevel = optionalIntParam(request, "userTrustLevel");
        int userPage = intParam(request, "userPage", 1);

        request.setAttribute("stats", adminService.stats());
        request.setAttribute("topicsPage", adminService.adminTopics(topicCriteria, currentUserId(request)));
        request.setAttribute("usersPage", adminService.adminUsers(userPage, userKeyword, userStatus, userRole, userTrustLevel));
        request.setAttribute("roles", roleService.listRoles());
        exposeFilters(request, topicCriteria, userKeyword, userStatus, userRole, userTrustLevel, userPage);
        request.setAttribute("pageTitle", "管理后台");
        request.setAttribute("activeNav", "admin");
        render(request, response, "admin/dashboard.jsp");
    }

    private SearchCriteria topicCriteria(HttpServletRequest request) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(intParam(request, "topicPage", 1));
        criteria.setSize(12);
        criteria.setKeyword(stringParam(request, "topicKeyword"));
        criteria.setCategoryId(longParam(request, "topicCategoryId"));
        criteria.setOrderBy(normalizedSelect(request, "topicOrder", "LATEST"));
        criteria.setCreatedWithinDays(optionalIntParam(request, "topicAgeDays"));
        criteria.setMinReplies(optionalIntParam(request, "topicMinReplies"));
        criteria.setMinViews(optionalIntParam(request, "topicMinViews"));
        return criteria;
    }

    private void exposeFilters(HttpServletRequest request, SearchCriteria topicCriteria, String userKeyword,
                               String userStatus, String userRole, Integer userTrustLevel, int userPage) {
        request.setAttribute("topicKeyword", topicCriteria.getKeyword());
        request.setAttribute("topicCategoryId", topicCriteria.getCategoryId());
        request.setAttribute("topicOrder", topicCriteria.getOrderBy());
        request.setAttribute("topicAgeDays", topicCriteria.getCreatedWithinDays());
        request.setAttribute("topicMinReplies", topicCriteria.getMinReplies());
        request.setAttribute("topicMinViews", topicCriteria.getMinViews());
        request.setAttribute("userKeyword", userKeyword);
        request.setAttribute("userStatus", userStatus);
        request.setAttribute("userRole", userRole);
        request.setAttribute("userTrustLevel", userTrustLevel);

        Map<String, String> allFilters = new LinkedHashMap<>();
        addTopicFilters(allFilters, topicCriteria);
        addUserFilters(allFilters, userKeyword, userStatus, userRole, userTrustLevel, userPage);

        Map<String, String> topicPager = new LinkedHashMap<>(allFilters);
        topicPager.remove("topicPage");
        request.setAttribute("topicPageBaseUrl", request.getContextPath() + "/admin/dashboard?" + query(topicPager) + (topicPager.isEmpty() ? "" : "&") + "topicPage=");

        Map<String, String> userPager = new LinkedHashMap<>(allFilters);
        userPager.remove("userPage");
        request.setAttribute("userPageBaseUrl", request.getContextPath() + "/admin/dashboard?" + query(userPager) + (userPager.isEmpty() ? "" : "&") + "userPage=");

        String current = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            current += "?" + request.getQueryString();
        }
        request.setAttribute("currentDashboardUrl", current);
    }

    private void addTopicFilters(Map<String, String> params, SearchCriteria criteria) {
        put(params, "topicKeyword", criteria.getKeyword());
        put(params, "topicCategoryId", criteria.getCategoryId());
        put(params, "topicOrder", criteria.getOrderBy());
        put(params, "topicAgeDays", criteria.getCreatedWithinDays());
        put(params, "topicMinReplies", criteria.getMinReplies());
        put(params, "topicMinViews", criteria.getMinViews());
        put(params, "topicPage", criteria.getPage());
    }

    private void addUserFilters(Map<String, String> params, String keyword, String status, String role, Integer trustLevel, int userPage) {
        put(params, "userKeyword", keyword);
        put(params, "userStatus", status);
        put(params, "userRole", role);
        put(params, "userTrustLevel", trustLevel);
        put(params, "userPage", userPage);
    }

    private String normalizedSelect(HttpServletRequest request, String name, String fallback) {
        String value = stringParam(request, name);
        return value.isBlank() ? fallback : value;
    }

    private Integer optionalIntParam(HttpServletRequest request, String name) {
        String value = stringParam(request, name);
        if (value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void put(Map<String, String> params, String key, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank() || "ALL".equalsIgnoreCase(text)) {
            return;
        }
        params.put(key, text);
    }

    private String query(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
