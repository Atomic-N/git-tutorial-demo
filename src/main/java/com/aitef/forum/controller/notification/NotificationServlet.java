package com.aitef.forum.controller.notification;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.NotificationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notifications")
public class NotificationServlet extends BaseServlet {
    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        request.setAttribute("notificationsPage", notificationService.list(currentUser(request).getId(), intParam(request, "page", 1), 20));
        request.setAttribute("pageTitle", "通知");
        request.setAttribute("activeNav", "notifications");
        render(request, response, "notification/list.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        notificationService.markAllRead(currentUser(request).getId());
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}
