package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.ReportService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/reports")
public class AdminReportServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        adminService.requireAdmin(currentUser(request));
        String status = stringParam(request, "status");
        request.setAttribute("status", status.isBlank() ? "PENDING" : status);
        request.setAttribute("reportsPage", reportService.listReports(status.isBlank() ? "PENDING" : status, intParam(request, "page", 1), 20));
        request.setAttribute("pageTitle", "举报处理");
        request.setAttribute("activeNav", "admin");
        render(request, response, "admin/reports.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        adminService.requireAdmin(currentUser(request));
        try {
            if ("resolve-hide".equals(stringParam(request, "action"))) {
                reportService.resolveAndHide(currentUserId(request), longParam(request, "reportId"));
                response.sendRedirect(request.getContextPath() + "/admin/reports?message=" + URLEncoder.encode("内容已隐藏，举报已标记处理", StandardCharsets.UTF_8));
                return;
            }
            reportService.updateStatus(currentUserId(request), longParam(request, "reportId"), stringParam(request, "status"));
            response.sendRedirect(request.getContextPath() + "/admin/reports?message=" + URLEncoder.encode("处理状态已更新", StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            response.sendRedirect(request.getContextPath() + "/admin/reports?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
