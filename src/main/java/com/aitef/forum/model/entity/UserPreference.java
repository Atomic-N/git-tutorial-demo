package com.aitef.forum.model.entity;

public class UserPreference {
    private Long userId;
    private boolean notifyOnReply = true;
    private boolean notifyOnLike = true;
    private boolean emailDigest = false;
    private boolean emailOnMention = true;
    private String theme = "SYSTEM";
    private String editorMode = "RICH";
    private boolean autoSaveDrafts = true;
    private boolean hideReadTopics = false;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isNotifyOnReply() {
        return notifyOnReply;
    }

    public void setNotifyOnReply(boolean notifyOnReply) {
        this.notifyOnReply = notifyOnReply;
    }

    public boolean isNotifyOnLike() {
        return notifyOnLike;
    }

    public void setNotifyOnLike(boolean notifyOnLike) {
        this.notifyOnLike = notifyOnLike;
    }

    public boolean isEmailDigest() {
        return emailDigest;
    }

    public void setEmailDigest(boolean emailDigest) {
        this.emailDigest = emailDigest;
    }

    public boolean isEmailOnMention() {
        return emailOnMention;
    }

    public void setEmailOnMention(boolean emailOnMention) {
        this.emailOnMention = emailOnMention;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getEditorMode() {
        return editorMode;
    }

    public void setEditorMode(String editorMode) {
        this.editorMode = editorMode;
    }

    public boolean isAutoSaveDrafts() {
        return autoSaveDrafts;
    }

    public void setAutoSaveDrafts(boolean autoSaveDrafts) {
        this.autoSaveDrafts = autoSaveDrafts;
    }

    public boolean isHideReadTopics() {
        return hideReadTopics;
    }

    public void setHideReadTopics(boolean hideReadTopics) {
        this.hideReadTopics = hideReadTopics;
    }
}