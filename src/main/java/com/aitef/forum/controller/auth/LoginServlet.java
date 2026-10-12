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

@WebServlet("/login")
public class LoginServlet extends BaseServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "登录");
        request.setAttribute("redirect", stringParam(request, "redirect"));
        render(request, response, "auth/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String redirect = stringParam(request, "redirect");
        try {
            User user = authService.login(stringParam(request, "account"), request.getParameter("password"));//调用authService。iogin
            request.getSession(true).setAttribute(CURRENT_USER, user);//把对象写到session
            response.sendRedirect(redirect.isBlank() ? request.getContextPath() + "/latest" : redirect);//跳转到latest
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("redirect", redirect);
            request.setAttribute("pageTitle", "登录");
            render(request, response, "auth/login.jsp");
        }
    }
}
