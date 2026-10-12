package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.DraftDao;
import com.aitef.forum.model.entity.Draft;
import com.aitef.forum.util.ValidationException;

import java.util.Optional;

public class DraftService {
    private final DraftDao draftDao = new DraftDao();

    public Optional<Draft> find(Long userId, String draftType, Long targetId) {
        return draftDao.find(userId, normalizeType(draftType), zeroToNull(targetId));
    }

    public Long save(Long userId, String draftType, Long targetId, String title, String content) {
        String normalizedType = normalizeType(draftType);
        String safeTitle = trimToLength(title, 180);
        String safeContent = trimToLength(content, 20000);
        if (safeTitle.isBlank() && safeContent.isBlank()) {
            delete(userId, normalizedType, targetId);
            return null;
        }
        return draftDao.save(userId, normalizedType, zeroToNull(targetId), safeTitle, safeContent);
    }

    public void delete(Long userId, String draftType, Long targetId) {
        draftDao.delete(userId, normalizeType(draftType), zeroToNull(targetId));
    }

    private String normalizeType(String draftType) {
        String value = draftType == null ? "" : draftType.trim().toUpperCase();
        if (!"TOPIC".equals(value) && !"REPLY".equals(value)) {
            throw new ValidationException("草稿类型不正确");
        }
        return value;
    }

    private Long zeroToNull(Long targetId) {
        return targetId == null || targetId <= 0 ? null : targetId;
    }

    private String trimToLength(String value, int length) {
        String text = value == null ? "" : value.trim();
        return text.length() <= length ? text : text.substring(0, length);
    }
}