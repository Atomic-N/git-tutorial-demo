package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.DraftService;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/posts/create")
public class PostCreateServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();
    private final DraftService draftService = new DraftService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long topicId = longParam(request, "topicId");
        try {
            topicService.reply(
                    currentUser(request).getId(),
                    topicId,
                    longParam(request, "replyToPostId"),
                    request.getParameter("content")
            );
            draftService.delete(currentUserId(request), "REPLY", topicId);
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "#reply-box");
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}