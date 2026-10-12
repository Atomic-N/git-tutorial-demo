package com.aitef.forum.model.service;

import com.aitef.forum.model.dao.UserDao;
import com.aitef.forum.model.entity.User;
import com.aitef.forum.model.entity.UserPreference;
import com.aitef.forum.util.ValidationException;

import java.util.List;

public class UserService {
    private final UserDao userDao = new UserDao();
    private final UserPreferenceService userPreferenceService = new UserPreferenceService();

    public User findByUsername(String username) {
        return userDao.findByUsername(username)
                .orElseThrow(() -> new ValidationException("用户不存在"));
    }

    public User findById(Long id) {
        return userDao.findById(id)
                .orElseThrow(() -> new ValidationException("用户不存在"));
    }

    public List<User> listActiveUsers(int limit) {
        return userDao.listActiveUsers(limit);
    }

    public UserPreference findPreference(Long userId) {
        return userPreferenceService.findByUserId(userId);
    }

    public UserPreference updatePreference(Long userId,
                                           boolean notifyOnReply,
                                           boolean notifyOnLike,
                                           boolean emailDigest,
                                           boolean emailOnMention,
                                           String theme,
                                           String editorMode,
                                           boolean autoSaveDrafts,
                                           boolean hideReadTopics) {
        return userPreferenceService.update(userId, notifyOnReply, notifyOnLike, emailDigest, emailOnMention,
                theme, editorMode, autoSaveDrafts, hideReadTopics);
    }

    public User updateProfile(Long userId, String displayName, String bio, String location, String website, String avatarUrl) {
        User user = findById(userId);
        displayName = displayName == null ? "" : displayName.trim();
        if (displayName.length() < 2 || displayName.length() > 60) {
            throw new ValidationException("昵称长度需要在 2-60 个字符之间");
        }
        user.setDisplayName(displayName);
        user.setBio(trimToLength(bio, 1000));
        user.setLocation(trimToLength(location, 80));
        user.setWebsite(trimToLength(website, 160));
        user.setAvatarUrl(trimToLength(avatarUrl, 255));
        userDao.updateProfile(user);
        return findById(userId);
    }

    private String trimToLength(String value, int length) {
        String text = value == null ? "" : value.trim();
        return text.length() <= length ? text : text.substring(0, length);
    }
}
