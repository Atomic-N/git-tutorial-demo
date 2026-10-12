package com.aitef.forum.filter;

import com.aitef.forum.model.entity.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
//需要登录访问的页面
@WebFilter(urlPatterns = {"/topics/new", "/topics/create", "/topics/edit", "/topics/delete", "/posts/create", "/posts/edit", "/posts/delete", "/bookmarks", "/notifications", "/reports/*", "/admin/*", "/api/*"})
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Object currentUser = httpRequest.getSession(false) == null ? null : httpRequest.getSession(false).getAttribute("currentUser");
        if (currentUser instanceof User) {
            chain.doFilter(request, response);
            return;
        }
        if (httpRequest.getRequestURI().contains("/api/")) {//API 未登录返回 JSON
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");//普通页面未登录重定向到登录页
            return;
        }
        String redirect = httpRequest.getRequestURI();
        if (httpRequest.getQueryString() != null) {
            redirect += "?" + httpRequest.getQueryString();
        }
        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }
}
