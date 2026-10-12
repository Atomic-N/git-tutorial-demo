package com.aitef.forum.controller.user;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.service.BadgeService;
import com.aitef.forum.model.service.DirectMessageService;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.model.service.UploadService;
import com.aitef.forum.model.service.UserRelationshipService;
import com.aitef.forum.model.service.UserService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/u/*")
public class UserProfileServlet extends BaseServlet {
    private final UserService userService = new UserService();
    private final TopicService topicService = new TopicService();
    private final BadgeService badgeService = new BadgeService();
    private final UploadService uploadService = new UploadService();
    private final UserRelationshipService relationshipService = new UserRelationshipService();
    private final DirectMessageService directMessageService = new DirectMessageService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String[] parts = parts(request);
        if (parts.length == 0 || parts[0].isBlank()) {
            if (currentUser(request) != null) {
                response.sendRedirect(request.getContextPath() + "/u/" + currentUser(request).getUsername());
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }
        User profile = userService.findByUsername(parts[0]);
        if (parts.length > 1 && "preferences".equals(parts[1])) {
            if (!canEdit(request, profile)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("profile", profile);
            request.setAttribute("preference", userService.findPreference(profile.getId()));
            request.setAttribute("uploads", uploadService.listRecent(profile.getId(), 12));
            request.setAttribute("pageTitle", "偏好设置 - " + profile.getDisplayName());
            request.setAttribute("activeNav", "profile");
            render(request, response, "user/preferences.jsp");
            return;
        }
        if (parts.length > 1 && "badges".equals(parts[1])) {
            request.setAttribute("profile", profile);
            request.setAttribute("badges", badgeService.listByUserId(profile.getId()));
            request.setAttribute("badgeCatalog", badgeService.listCatalogForUser(profile.getId()));
            request.setAttribute("pageTitle", "徽章 - " + profile.getDisplayName());
            request.setAttribute("activeNav", "profile");
            render(request, response, "user/badges.jsp");
            return;
        }

        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(intParam(request, "page", 1));
        criteria.setSize(10);
        request.setAttribute("profile", profile);
        request.setAttribute("badges", badgeService.listByUserId(profile.getId()));
        request.setAttribute("topicsPage", topicService.list("latest", criteria, profile.getId(), currentUserId(request)));
        request.setAttribute("relationshipState", relationshipService.state(currentUserId(request), profile.getId()));
        request.setAttribute("followingUsers", relationshipService.following(profile.getId()));
        boolean privateMessagesEnabled = Boolean.TRUE.equals(request.getAttribute("allowPrivateMessages"));
        boolean blockedByViewer = Boolean.TRUE.equals(request.getAttribute("relationshipState") instanceof java.util.Map<?, ?> state ? state.get("blocked") : false);
        request.setAttribute("allowPrivateMessages", privateMessagesEnabled && !blockedByViewer);
        request.setAttribute("pageTitle", profile.getDisplayName());
        request.setAttribute("activeNav", "profile");
        render(request, response, "user/profile.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        String[] parts = parts(request);
        User profile = userService.findByUsername(parts[0]);
        if (!canEdit(request, profile)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            User updated = userService.updateProfile(
                    profile.getId(),
                    stringParam(request, "displayName"),
                    request.getParameter("bio"),
                    stringParam(request, "location"),
                    stringParam(request, "website"),
                    stringParam(request, "avatarUrl")
            );
            userService.updatePreference(
                    profile.getId(),
                    checked(request, "notifyOnReply"),
                    checked(request, "notifyOnLike"),
                    checked(request, "emailDigest"),
                    checked(request, "emailOnMention"),
                    stringParam(request, "theme"),
                    stringParam(request, "editorMode"),
                    checked(request, "autoSaveDrafts"),
                    checked(request, "hideReadTopics")
            );
            request.getSession().setAttribute(CURRENT_USER, updated);
            response.sendRedirect(request.getContextPath() + "/u/" + updated.getUsername() + "/preferences?message=" + java.net.URLEncoder.encode("偏好设置已保存", java.nio.charset.StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("profile", profile);
            request.setAttribute("preference", userService.findPreference(profile.getId()));
            request.setAttribute("uploads", uploadService.listRecent(profile.getId(), 12));
            request.setAttribute("pageTitle", "偏好设置 - " + profile.getDisplayName());
            render(request, response, "user/preferences.jsp");
        }
    }

    private boolean checked(HttpServletRequest request, String name) {
        return request.getParameter(name) != null;
    }

    private boolean canEdit(HttpServletRequest request, User profile) {
        User current = currentUser(request);
        return current != null && (current.getId().equals(profile.getId()) || current.isAdmin());
    }

    private String[] parts(HttpServletRequest request) {
        String path = pathAfter(request, "/u");
        return path.isBlank() ? new String[0] : path.split("/");
    }
}
