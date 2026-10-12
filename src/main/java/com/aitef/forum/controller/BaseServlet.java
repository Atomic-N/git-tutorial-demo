package com.aitef.forum.controller;

import com.aitef.forum.model.dto.ApiResponse;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.service.CatalogService;
import com.aitef.forum.model.service.DirectMessageService;
import com.aitef.forum.model.service.NotificationService;
import com.aitef.forum.model.service.SiteSettingService;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.model.service.UserPreferenceService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public abstract class BaseServlet extends HttpServlet {
    protected static final String CURRENT_USER = "currentUser";
    private static final String CSRF_TOKEN = "csrfToken";
    private static final String ASSET_VERSION = Long.toString(System.currentTimeMillis());
    private final CatalogService catalogService = new CatalogService();
    private final NotificationService notificationService = new NotificationService();
    private final UserPreferenceService userPreferenceService = new UserPreferenceService();
    private final SiteSettingService siteSettingService = new SiteSettingService();
    private final DirectMessageService directMessageService = new DirectMessageService();
    private final TopicService topicService = new TopicService();
    private final Gson gson = new Gson();

    protected User currentUser(HttpServletRequest request) {
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute(CURRENT_USER);
        return value instanceof User user ? user : null;
    }

    protected Long currentUserId(HttpServletRequest request) {
        User user = currentUser(request);
        return user == null ? null : user.getId();
    }

    protected boolean requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (currentUser(request) != null) {
            return true;
        }
        String redirect = request.getRequestURI();
        if (request.getQueryString() != null) {
            redirect += "?" + request.getQueryString();
        }
        response.sendRedirect(request.getContextPath() + "/login?redirect=" + urlEncode(redirect));
        return false;
    }

    protected void render(HttpServletRequest request, HttpServletResponse response, String jsp) throws ServletException, IOException {
        User currentUser = currentUser(request);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        request.setAttribute("currentUser", currentUser);
        request.setAttribute("csrfToken", ensureCsrfToken(request));
        request.setAttribute("siteName", siteSettingService.value("site_name"));
        request.setAttribute("assetVersion", ASSET_VERSION);
        request.setAttribute("noticeText", siteSettingService.value("notice_text"));
        request.setAttribute("communityGuidelinesUrl", siteSettingService.value("community_guidelines_url"));
        request.setAttribute("allowPrivateMessages", siteSettingService.booleanValue("allow_private_messages"));
        request.setAttribute("categories", currentUser != null && currentUser.isAdmin()
                ? catalogService.listCategories()
                : catalogService.listVisibleCategories(currentUser == null ? 0 : currentUser.getTrustLevel()));
        Long userId = currentUserId(request);
        int viewerTrustLevel = currentUser == null ? 0 : (currentUser.isAdmin() ? 99 : currentUser.getTrustLevel());
        request.setAttribute("unreadNotificationCount", userId == null ? 0 : notificationService.countUnread(userId));
        request.setAttribute("unreadDirectMessageCount", userId == null ? 0 : directMessageService.countUnreadThreads(userId));
        request.setAttribute("unreadTopicCount", userId == null ? 0 : topicService.unreadTopicCount(userId, viewerTrustLevel));
        request.setAttribute("unreadCategoryCounts", userId == null
                ? Map.of()
                : topicService.unreadTopicCountsByCategory(userId, viewerTrustLevel));
        if (userId != null) {
            try {
                request.setAttribute("currentPreference", userPreferenceService.findByUserId(userId));
            } catch (RuntimeException ignored) {
                request.setAttribute("currentPreference", null);
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/" + jsp).forward(request, response);
    }

    protected void json(HttpServletResponse response, ApiResponse<?> apiResponse) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(gson.toJson(apiResponse));
    }

    protected int intParam(HttpServletRequest request, String name, int defaultValue) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected Long longParam(HttpServletRequest request, String name) {
        try {
            String value = request.getParameter(name);
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    protected String stringParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    protected String pathAfter(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (!uri.startsWith(prefix)) {
            return "";
        }
        String path = uri.substring(prefix.length());
        return path.startsWith("/") ? path.substring(1) : path;
    }

    protected String ensureCsrfToken(HttpServletRequest request) {
        var session = request.getSession(true);
        Object existing = session.getAttribute(CSRF_TOKEN);
        if (existing instanceof String token && !token.isBlank()) {
            return token;
        }
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN, token);
        return token;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
