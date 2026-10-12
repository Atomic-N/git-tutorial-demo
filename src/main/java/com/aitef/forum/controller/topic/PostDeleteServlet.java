package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/posts/delete")
public class PostDeleteServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long postId = longParam(request, "postId");
        Long topicId = longParam(request, "topicId");
        try {
            topicService.deleteReply(currentUserId(request), postId);
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?message=" + URLEncoder.encode("回复已删除", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
