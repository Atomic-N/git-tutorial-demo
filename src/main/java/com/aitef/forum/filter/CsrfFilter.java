package com.aitef.forum.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class CsrfFilter implements Filter {
    private static final String CSRF_TOKEN = "csrfToken";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(CSRF_TOKEN);
        String actual = headerOrParam(httpRequest);
        if (expected != null && expected.equals(actual)) {
            chain.doFilter(request, response);
            return;
        }

        if (httpRequest.getRequestURI().contains("/api/")) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":403,\"message\":\"CSRF 校验失败\"}");
            return;
        }
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF 校验失败");
    }

    private String headerOrParam(HttpServletRequest request) {
        String header = request.getHeader("X-CSRF-Token");
        if (header != null && !header.isBlank()) {
            return header;
        }
        String param = request.getParameter("csrfToken");
        return param == null || param.isBlank() ? null : param;
    }
}
