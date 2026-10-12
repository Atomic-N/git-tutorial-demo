package com.aitef.forum.controller.admin;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.AdminService;
import com.aitef.forum.model.service.SiteSettingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/admin/settings")
public class AdminSettingServlet extends BaseServlet {
    private final AdminService adminService = new AdminService();
    private final SiteSettingService siteSettingService = new SiteSettingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        adminService.requireAdmin(currentUser(request));
        request.setAttribute("settingsMap", siteSettingService.allAsMap());
        request.setAttribute("pageTitle", "站点配置");
        request.setAttribute("activeNav", "admin");
        render(request, response, "admin/settings.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        adminService.requireAdmin(currentUser(request));
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("site_name", stringParam(request, "siteName"));
        updates.put("notice_text", request.getParameter("noticeText") == null ? "" : request.getParameter("noticeText").trim());
        String guidelinesUrl = stringParam(request, "communityGuidelinesUrl");
        if (guidelinesUrl.isBlank()) {
            guidelinesUrl = "/t/1/welcome-to-ai-tef";
        }
        updates.put("community_guidelines_url", guidelinesUrl.startsWith("/") ? guidelinesUrl : "/" + guidelinesUrl);
        updates.put("allow_private_messages", request.getParameter("allowPrivateMessages") != null ? "true" : "false");
        updates.put("markdown_enabled", request.getParameter("markdownEnabled") != null ? "true" : "false");
        siteSettingService.saveAll(currentUserId(request), updates);
        response.sendRedirect(request.getContextPath() + "/admin/settings?message=" + URLEncoder.encode("站点配置已保存", StandardCharsets.UTF_8));
    }
}
