package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.ApiResponse;
import com.aitef.forum.model.service.InteractionService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/interactions/*")
public class InteractionApiServlet extends BaseServlet {
    private final InteractionService interactionService = new InteractionService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (currentUser(request) == null) {
            json(response, ApiResponse.fail("请先登录"));
            return;
        }
        Long id = longParam(request, "id");
        String action = pathAfter(request, "/api/interactions");
        try {
            Object result = switch (action) {
                case "topic-like" -> interactionService.toggleTopicLike(currentUser(request).getId(), id);
                case "post-like" -> interactionService.togglePostLike(currentUser(request).getId(), id);
                case "topic-bookmark" -> interactionService.toggleTopicBookmark(currentUser(request).getId(), id);
                default -> throw new ValidationException("未知操作");
            };
            json(response, ApiResponse.ok(result));
        } catch (ValidationException e) {
            json(response, ApiResponse.fail(e.getMessage()));
        }
    }
}
