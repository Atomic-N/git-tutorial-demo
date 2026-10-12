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

@WebFilter("/admin/*")
public class AdminFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Object value = httpRequest.getSession(false) == null ? null : httpRequest.getSession(false).getAttribute("currentUser");
        if (value instanceof User user && user.isAdmin()) {
            chain.doFilter(request, response);
            return;
        }
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
