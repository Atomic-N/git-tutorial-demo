package com.aitef.forum.model.dao;

import com.aitef.forum.model.entity.UserPreference;
import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserPreferenceDao {
    public UserPreference findByUserId(Long userId) {
        String sql = "SELECT * FROM user_preferences WHERE user_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
            UserPreference preference = defaults(userId);
            save(preference);
            return preference;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load user preferences", e);
        }
    }

    public void save(UserPreference preference) {
        String sql = """
                INSERT INTO user_preferences (user_id, notify_on_reply, notify_on_like, email_digest, email_on_mention, theme, editor_mode, auto_save_drafts, hide_read_topics)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    notify_on_reply = VALUES(notify_on_reply),
                    notify_on_like = VALUES(notify_on_like),
                    email_digest = VALUES(email_digest),
                    email_on_mention = VALUES(email_on_mention),
                    theme = VALUES(theme),
                    editor_mode = VALUES(editor_mode),
                    auto_save_drafts = VALUES(auto_save_drafts),
                    hide_read_topics = VALUES(hide_read_topics)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, preference.getUserId());
            ps.setBoolean(2, preference.isNotifyOnReply());
            ps.setBoolean(3, preference.isNotifyOnLike());
            ps.setBoolean(4, preference.isEmailDigest());
            ps.setBoolean(5, preference.isEmailOnMention());
            ps.setString(6, preference.getTheme());
            ps.setString(7, preference.getEditorMode());
            ps.setBoolean(8, preference.isAutoSaveDrafts());
            ps.setBoolean(9, preference.isHideReadTopics());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save user preferences", e);
        }
    }

    private UserPreference defaults(Long userId) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        return preference;
    }

    private UserPreference map(ResultSet rs) throws Exception {
        UserPreference preference = new UserPreference();
        preference.setUserId(rs.getLong("user_id"));
        preference.setNotifyOnReply(rs.getBoolean("notify_on_reply"));
        preference.setNotifyOnLike(rs.getBoolean("notify_on_like"));
        preference.setEmailDigest(rs.getBoolean("email_digest"));
        preference.setEmailOnMention(rs.getBoolean("email_on_mention"));
        preference.setTheme(rs.getString("theme"));
        preference.setEditorMode(rs.getString("editor_mode"));
        preference.setAutoSaveDrafts(rs.getBoolean("auto_save_drafts"));
        preference.setHideReadTopics(rs.getBoolean("hide_read_topics"));
        return preference;
    }
}