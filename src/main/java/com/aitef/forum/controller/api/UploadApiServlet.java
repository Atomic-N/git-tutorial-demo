package com.aitef.forum.controller.api;

import com.aitef.forum.controller.BaseServlet;
import com.aitef.forum.model.dto.ApiResponse;
import com.aitef.forum.model.entity.UploadedFile;
import com.aitef.forum.model.service.UploadService;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/uploads")
@MultipartConfig(maxFileSize = 10L * 1024L * 1024L, maxRequestSize = 12L * 1024L * 1024L)
public class UploadApiServlet extends BaseServlet {
    private final UploadService uploadService = new UploadService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!requireLogin(request, response)) {
            return;
        }
        try {
            Part filePart = request.getPart("file");
            UploadedFile file = uploadService.upload(getServletContext(), currentUserId(request), filePart);
            json(response, ApiResponse.ok(Map.of(
                    "id", file.getId(),
                    "originalName", file.getOriginalName(),
                    "url", request.getContextPath() + file.getPublicUrl(),
                    "contentType", file.getContentType(),
                    "fileSize", file.getFileSize()
            )));
        } catch (ValidationException e) {
            json(response, ApiResponse.fail(e.getMessage()));
        }
    }
}