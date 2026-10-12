package com.aitef.forum.controller.message;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.DirectMessageThread;
import com.aitef.forum.model.service.DirectMessageService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/messages", "/messages/*"})
public class DirectMessageServlet extends BaseServlet {
    private final DirectMessageService directMessageService = new DirectMessageService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        List<DirectMessageThread> threads = directMessageService.listThreads(currentUserId(request));
        request.setAttribute("threads", threads);
        request.setAttribute("composeTo", stringParam(request, "to"));
        String path = pathAfter(request, "/messages");
        if (!path.isBlank()) {
            try {
                Long threadId = Long.parseLong(path.split("/")[0]);
                DirectMessageThread thread = directMessageService.getThread(threadId, currentUserId(request));
                request.setAttribute("thread", thread);
                request.setAttribute("messages", directMessageService.listMessages(threadId, currentUserId(request)));
            } catch (ValidationException e) {
                response.sendRedirect(request.getContextPath() + "/messages?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
                return;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        request.setAttribute("pageTitle", "私信");
        request.setAttribute("activeNav", "messages");
        render(request, response, "message/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            String username = stringParam(request, "username");
            String subject = stringParam(request, "subject");
            String body = request.getParameter("content");
            String quotedIdStr = request.getParameter("quotedMessageId");
            Long quotedMessageId = null;
            if (quotedIdStr != null && !quotedIdStr.isBlank()) {
                try {
                    quotedMessageId = Long.parseLong(quotedIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的引用ID
                }
            }
            Long threadId = directMessageService.sendToUser(currentUserId(request), username, subject, body, quotedMessageId);
            response.sendRedirect(request.getContextPath() + "/messages/" + threadId);
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/messages?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
