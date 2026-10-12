package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.TopicDetail;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/t/*")
public class TopicDetailServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long topicId = parseTopicId(request);
        if (topicId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            TopicDetail detail = topicService.getDetail(topicId, currentUserId(request));
            request.setAttribute("detail", detail);
            request.setAttribute("pageTitle", detail.getTopic().getTitle());
            request.setAttribute("activeNav", "topics");
            render(request, response, "topic/detail.jsp");
        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private Long parseTopicId(HttpServletRequest request) {
        String path = pathAfter(request, "/t");
        if (path.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(path.split("/")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
