package com.aitef.forum.controller.user;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.service.UserRelationshipService;
import com.aitef.forum.model.service.UserService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/u/relation")
public class UserRelationshipServlet extends BaseServlet {
    private final UserRelationshipService relationshipService = new UserRelationshipService();
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        String username = stringParam(request, "username");
        User target = userService.findByUsername(username);
        String action = stringParam(request, "action");
        try {
            switch (action) {
                case "follow" -> relationshipService.follow(currentUserId(request), target.getId());
                case "unfollow" -> relationshipService.unfollow(currentUserId(request), target.getId());
                case "mute" -> relationshipService.mute(currentUserId(request), target.getId(), true);
                case "unmute" -> relationshipService.mute(currentUserId(request), target.getId(), false);
                case "block" -> relationshipService.block(currentUserId(request), target.getId(), true);
                case "unblock" -> relationshipService.block(currentUserId(request), target.getId(), false);
                default -> throw new ValidationException("未知关系操作");
            }
            response.sendRedirect(request.getContextPath() + "/u/" + target.getUsername() + "?message=" + URLEncoder.encode("关系设置已更新", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/u/" + target.getUsername() + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
