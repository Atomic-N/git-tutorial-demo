package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.UserPreferenceDao;
import com.aitef.forum.model.entity.UserPreference;
import com.aitef.forum.util.ValidationException;

import java.util.Set;

public class UserPreferenceService {
    private static final Set<String> THEMES = Set.of("SYSTEM", "LIGHT", "DARK");
    private static final Set<String> EDITOR_MODES = Set.of("RICH", "MARKDOWN", "PLAIN");

    private final UserPreferenceDao userPreferenceDao = new UserPreferenceDao();

    public UserPreference findByUserId(Long userId) {
        return userPreferenceDao.findByUserId(userId);
    }

    public UserPreference update(Long userId,
                                 boolean notifyOnReply,
                                 boolean notifyOnLike,
                                 boolean emailDigest,
                                 boolean emailOnMention,
                                 String theme,
                                 String editorMode,
                                 boolean autoSaveDrafts,
                                 boolean hideReadTopics) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setNotifyOnReply(notifyOnReply);
        preference.setNotifyOnLike(notifyOnLike);
        preference.setEmailDigest(emailDigest);
        preference.setEmailOnMention(emailOnMention);
        preference.setTheme(normalizeChoice(theme, THEMES, "SYSTEM"));
        preference.setEditorMode(normalizeChoice(editorMode, EDITOR_MODES, "RICH"));
        preference.setAutoSaveDrafts(autoSaveDrafts);
        preference.setHideReadTopics(hideReadTopics);
        userPreferenceDao.save(preference);
        return findByUserId(userId);
    }

    private String normalizeChoice(String value, Set<String> allowed, String fallback) {
        String normalized = value == null ? fallback : value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new ValidationException("偏好设置参数不正确");
        }
        return normalized;
    }
}