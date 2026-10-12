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

@WebServlet("/topics/solution")
public class TopicSolutionServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long topicId = longParam(request, "topicId");
        try {
            topicService.markSolution(topicId, longParam(request, "postId"), currentUserId(request), request.getParameter("clear") == null);
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?message=" + URLEncoder.encode("已更新解决状态", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
