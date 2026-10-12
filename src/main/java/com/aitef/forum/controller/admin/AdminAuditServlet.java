package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/audit")
public class AdminAuditServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        adminService.requireAdmin(currentUser(request));
        request.setAttribute("auditPage", auditLogService.list(intParam(request, "page", 1), 20));
        request.setAttribute("pageTitle", "审计日志");
        request.setAttribute("activeNav", "admin");
        render(request, response, "admin/audit.jsp");
    }
}
