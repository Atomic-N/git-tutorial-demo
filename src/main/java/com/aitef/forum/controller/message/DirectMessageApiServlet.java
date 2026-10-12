package com.aitef.forum.controller.message;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.service.DirectMessageService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 私信会话管理 API
 * 支持置顶、归档、免打扰、删除等操作
 */
@WebServlet(urlPatterns = {"/api/messages/threads/*"})
public class DirectMessageApiServlet extends BaseServlet {
    private final DirectMessageService directMessageService = new DirectMessageService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }

        String path = pathAfter(request, "/api/messages/threads");
        String[] parts = path.split("/");

        if (parts.length < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request path");
            return;
        }

        try {
            Long threadId = Long.parseLong(parts[0]);
            String action = parts[1];

            switch (action) {
                case "pin":
                    directMessageService.pinThread(threadId, currentUserId(request));
                    jsonSuccess(response, "会话已置顶");
                    break;
                case "unpin":
                    directMessageService.unpinThread(threadId, currentUserId(request));
                    jsonSuccess(response, "已取消置顶");
                    break;
                case "archive":
                    directMessageService.archiveThread(threadId, currentUserId(request));
                    jsonSuccess(response, "会话已归档");
                    break;
                case "unarchive":
                    directMessageService.unarchiveThread(threadId, currentUserId(request));
                    jsonSuccess(response, "已取消归档");
                    break;
                case "mute":
                    directMessageService.muteThread(threadId, currentUserId(request));
                    jsonSuccess(response, "已开启免打扰");
                    break;
                case "unmute":
                    directMessageService.unmuteThread(threadId, currentUserId(request));
                    jsonSuccess(response, "已关闭免打扰");
                    break;
                case "delete":
                    directMessageService.deleteThread(threadId, currentUserId(request));
                    jsonSuccess(response, "会话已删除");
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown action");
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid thread ID");
        } catch (ValidationException e) {
            jsonError(response, e.getMessage());
        } catch (Exception e) {
            jsonError(response, "操作失败");
        }
    }

    private void jsonSuccess(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":true,\"message\":\"" + message + "\"}");
    }

    private void jsonError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
