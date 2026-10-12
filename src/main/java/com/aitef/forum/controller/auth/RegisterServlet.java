package com.aitef.forum.controller.auth;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.service.AuthService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "注册");
        render(request, response, "auth/register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            User user = authService.register(
                    stringParam(request, "username"),
                    stringParam(request, "email"),
                    request.getParameter("password"),
                    stringParam(request, "displayName")
            );
            request.getSession(true).setAttribute(CURRENT_USER, user);
            response.sendRedirect(request.getContextPath() + "/latest");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("pageTitle", "注册");
            render(request, response, "auth/register.jsp");
        }
    }
}
