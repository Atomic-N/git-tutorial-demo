package com.aitef.forum.controller.badge;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.BadgeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/badges")
public class BadgeServlet extends BaseServlet {
    private final BadgeService badgeService = new BadgeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("badges", badgeService.listCatalogForUser(currentUserId(request)));
        request.setAttribute("pageTitle", "徽章");
        request.setAttribute("activeNav", "badges");
        render(request, response, "badge/index.jsp");
    }
}