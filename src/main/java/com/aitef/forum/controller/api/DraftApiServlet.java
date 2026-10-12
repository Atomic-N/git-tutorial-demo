package com.aitef.forum.controller.api;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.ApiResponse;
import com.aitef.forum.model.entity.Draft;
import com.aitef.forum.model.service.DraftService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/drafts")
public class DraftApiServlet extends BaseServlet {
    private final DraftService draftService = new DraftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            Draft draft = draftService.find(currentUserId(request), stringParam(request, "type"), longParam(request, "targetId")).orElse(null);
            if (draft == null) {
                json(response, ApiResponse.ok(Map.of("exists", false)));
            } else {
                json(response, ApiResponse.ok(Map.of(
                        "exists", true,
                        "id", draft.getId(),
                        "title", draft.getTitle(),
                        "content", draft.getContent(),
                        "updatedAgo", draft.getUpdatedAgo()
                )));
            }
        } catch (ValidationException e) {
            json(response, ApiResponse.fail(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            String action = stringParam(request, "action");
            String type = stringParam(request, "type");
            Long targetId = longParam(request, "targetId");
            if ("delete".equalsIgnoreCase(action)) {
                draftService.delete(currentUserId(request), type, targetId);
                json(response, ApiResponse.ok(Map.of("deleted", true)));
                return;
            }
            Long draftId = draftService.save(currentUserId(request), type, targetId, request.getParameter("title"), request.getParameter("content"));
            json(response, ApiResponse.ok(Map.of("saved", draftId != null, "id", draftId == null ? 0 : draftId)));
        } catch (ValidationException e) {
            json(response, ApiResponse.fail(e.getMessage()));
        }
    }
}