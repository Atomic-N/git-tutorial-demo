package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.SearchCriteria;
import com.aitef.forum.model.service.TopicService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/latest", "/new", "/unread", "/top", "/bookmarks"})
public class TopicListServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String mode = request.getServletPath().replace("/", "");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPage(intParam(request, "page", 1));
        criteria.setSize(20);
        request.setAttribute("topicsPage", topicService.list(mode, criteria, null, currentUserId(request)));
        request.setAttribute("mode", mode);
        request.setAttribute("activeNav", "topics");
        request.setAttribute("pageTitle", title(mode));
        render(request, response, "topic/list.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        if ("/unread".equals(request.getServletPath())) {
            topicService.markAllRead(currentUserId(request));
            response.sendRedirect(request.getContextPath() + "/unread?message=" + java.net.URLEncoder.encode("已将所有话题标记为已读", java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    private String title(String mode) {
        return switch (mode) {
            case "new" -> "新话题";
            case "unread" -> "未读话题";
            case "top" -> "排行榜";
            case "bookmarks" -> "书签话题";
            default -> "最新话题";
        };
    }
}
