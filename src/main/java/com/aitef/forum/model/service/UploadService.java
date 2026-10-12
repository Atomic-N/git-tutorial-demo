package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.UploadedFileDao;
import com.aitef.forum.model.entity.UploadedFile;
import com.aitef.forum.util.ValidationException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UploadService {
    private static final long MAX_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml",
            "application/pdf", "text/plain", "application/zip", "application/x-zip-compressed"
    );
    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("text/plain", ".txt"),
            Map.entry("application/zip", ".zip"),
            Map.entry("application/x-zip-compressed", ".zip")
    );

    private final UploadedFileDao uploadedFileDao = new UploadedFileDao();
    private final BadgeService badgeService = new BadgeService();

    public UploadedFile upload(ServletContext context, Long userId, Part part) {
        if (part == null || part.getSize() <= 0) {
            throw new ValidationException("请选择要上传的文件");
        }
        if (part.getSize() > MAX_SIZE) {
            throw new ValidationException("文件不能超过 10MB");
        }
        String contentType = normalizeContentType(part.getContentType());
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException("暂不支持该文件类型");
        }
        String originalName = safeOriginalName(part.getSubmittedFileName());
        LocalDate date = LocalDate.now();
        String relativeDir = "/static/uploads/%d/%02d".formatted(date.getYear(), date.getMonthValue());
        String storedName = UUID.randomUUID() + EXTENSIONS.getOrDefault(contentType, extensionFromName(originalName));
        String realDir = context.getRealPath(relativeDir);
        if (realDir == null) {
            throw new IllegalStateException("Cannot resolve upload directory");
        }
        Path dir = Path.of(realDir);
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(dir.normalize())) {
            throw new ValidationException("文件名不合法");
        }
        try {
            Files.createDirectories(dir);
            try (InputStream input = part.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        UploadedFile file = new UploadedFile();
        file.setUserId(userId);
        file.setOriginalName(originalName);
        file.setStoredName(storedName);
        file.setContentType(contentType);
        file.setFileSize(part.getSize());
        file.setPublicUrl(relativeDir + "/" + storedName);
        Long fileId = uploadedFileDao.create(file);
        file.setId(fileId);
        badgeService.refreshUserBadges(userId);
        return file;
    }

    public List<UploadedFile> listRecent(Long userId, int limit) {
        return uploadedFileDao.listByUserId(userId, limit);
    }

    private String normalizeContentType(String value) {
        return value == null ? "application/octet-stream" : value.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }

    private String safeOriginalName(String submittedFileName) {
        String name = submittedFileName == null ? "upload" : Path.of(submittedFileName).getFileName().toString();
        name = name.replaceAll("[\\r\\n\\t]", "_").trim();
        return name.isBlank() ? "upload" : (name.length() <= 180 ? name : name.substring(name.length() - 180));
    }

    private String extensionFromName(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return ".bin";
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        return ext.length() > 12 ? ".bin" : ext;
    }
}
