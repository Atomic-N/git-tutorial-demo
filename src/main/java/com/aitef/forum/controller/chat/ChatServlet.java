package com.aitef.forum.controller.chat;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.ChatChannel;
import com.aitef.forum.model.service.ChatService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@WebServlet(urlPatterns = {"/chat", "/chat/*"})
public class ChatServlet extends BaseServlet {
    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<ChatChannel> channels = chatService.listChannels(currentUser(request));
            if (channels.isEmpty()) {
                request.setAttribute("channels", channels);
                request.setAttribute("pageTitle", "聊天频道");
                request.setAttribute("activeNav", "chat");
                render(request, response, "chat/index.jsp");
                return;
            }
            String slug = pathAfter(request, "/chat");
            ChatChannel selected = slug.isBlank() ? channels.get(0) : chatService.getChannel(slug, currentUser(request));
            request.setAttribute("channels", channels);
            request.setAttribute("channel", selected);
            request.setAttribute("messages", chatService.listMessages(selected.getId()));
            request.setAttribute("pageTitle", selected.getName());
            request.setAttribute("activeNav", "chat");
            render(request, response, "chat/index.jsp");
        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            String slug = stringParam(request, "channel");
            chatService.sendMessage(currentUser(request), slug, longParam(request, "replyToMessageId"), request.getParameter("content"));
            response.sendRedirect(request.getContextPath() + "/chat/" + slug);
        } catch (ValidationException e) {
            String slug = stringParam(request, "channel");
            response.sendRedirect(request.getContextPath() + "/chat/" + slug + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
