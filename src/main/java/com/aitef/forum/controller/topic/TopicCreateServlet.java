package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.DraftService;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/topics/new", "/topics/create"})
public class TopicCreateServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();
    private final DraftService draftService = new DraftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        draftService.find(currentUserId(request), "TOPIC", null).ifPresent(draft -> request.setAttribute("draft", draft));
        request.setAttribute("pageTitle", "新建话题");
        request.setAttribute("activeNav", "topics");
        render(request, response, "topic/form.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            Long topicId = topicService.createTopic(
                    currentUser(request).getId(),
                    longParam(request, "categoryId"),
                    stringParam(request, "title"),
                    request.getParameter("content"),
                    request.getParameter("tags")
            );
            draftService.delete(currentUserId(request), "TOPIC", null);
            response.sendRedirect(request.getContextPath() + "/t/" + topicId);
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("pageTitle", "新建话题");
            render(request, response, "topic/form.jsp");
        }
    }
}