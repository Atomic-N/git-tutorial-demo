package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.Post;
import com.aitef.forum.model.service.TopicService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/posts/edit")
public class PostEditServlet extends BaseServlet {
    private final TopicService topicService = new TopicService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long postId = longParam(request, "postId");
        try {
            Post post = topicService.getEditablePost(postId, currentUserId(request));
            bindForm(request, post);
            request.setAttribute("pageTitle", "编辑回复");
            request.setAttribute("activeNav", "topics");
            render(request, response, "topic/post-edit.jsp");
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/latest?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        Long postId = longParam(request, "postId");
        Long topicId = longParam(request, "topicId");
        try {
            topicService.updateReply(currentUserId(request), postId, request.getParameter("content"));
            response.sendRedirect(request.getContextPath() + "/t/" + topicId + "#post-" + postId);
        } catch (ValidationException e) {
            Post post = topicService.getEditablePost(postId, currentUserId(request));
            bindForm(request, post);
            request.setAttribute("formContentValue", request.getParameter("content"));
            request.setAttribute("error", e.getMessage());
            request.setAttribute("pageTitle", "编辑回复");
            request.setAttribute("activeNav", "topics");
            render(request, response, "topic/post-edit.jsp");
        }
    }

    private void bindForm(HttpServletRequest request, Post post) {
        request.setAttribute("editablePost", post);
        request.setAttribute("cancelUrl", request.getContextPath() + "/t/" + post.getTopicId() + "#post-" + post.getId());
        request.setAttribute("formContentValue", post.getRawContent());
    }
}
