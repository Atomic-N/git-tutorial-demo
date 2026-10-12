package com.aitef.forum.controller.topic;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.ReportService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/reports/create")
public class ReportCreateServlet extends BaseServlet {
    private final ReportService reportService = new ReportService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        String targetType = stringParam(request, "targetType");
        Long targetId = longParam(request, "targetId");
        String back = stringParam(request, "back");
        try {
            reportService.createReport(currentUser(request), targetType, targetId, stringParam(request, "reason"), request.getParameter("detail"));
            response.sendRedirect(back.isBlank() ? request.getContextPath() + "/latest" : back + "?message=" + URLEncoder.encode("举报已提交", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(back.isBlank() ? request.getContextPath() + "/latest" : back + "?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
