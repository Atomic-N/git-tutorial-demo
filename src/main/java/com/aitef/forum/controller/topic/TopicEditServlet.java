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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/topics/edit")
public class TopicEditServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long topicId = longParam(request, "topicId");
        try {
            TopicDetail detail = topicService.getEditableDetail(topicId, currentUserId(request));
            bindForm(request, detail.getTopic().getId(), detail.getTopic().getTitle(), detail.getTopic().getCategoryId(), topicService.tagText(topicId), detail.getPosts().get(0).getRawContent());
            request.setAttribute("pageTitle", "编辑话题");
            request.setAttribute("activeNav", "topics");
            render(request, response, "topic/form.jsp");
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long topicId = longParam(request, "topicId");
        try {
            topicService.updateTopic(
                    currentUserId(request),
                    topicId,
                    longParam(request, "categoryId"),
                    stringParam(request, "title"),
                    request.getParameter("content"),
                    request.getParameter("tags")
            );
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "?message=" + URLEncoder.encode("话题已更新", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            bindForm(request, topicId, stringParam(request, "title"), longParam(request, "categoryId"), request.getParameter("tags"), request.getParameter("content"));
            request.setAttribute("error", e.getMessage());
            request.setAttribute("pageTitle", "编辑话题");
            request.setAttribute("activeNav", "topics");
            render(request, response, "topic/form.jsp");
        }
    }

    private void bindForm(HttpServletRequest request, Long topicId, String title, Long categoryId, String tags, String content) {
        request.setAttribute("editingTopic", true);
        request.setAttribute("topicEditId", topicId);
        request.setAttribute("cancelUrl", request.getContextPath() + "/t/" + topicId);
        request.setAttribute("formTitleValue", title);
        request.setAttribute("formCategoryIdValue", categoryId);
        request.setAttribute("formTagsValue", tags == null ? "" : tags);
        request.setAttribute("formContentValue", content == null ? "" : content);
    }
}
