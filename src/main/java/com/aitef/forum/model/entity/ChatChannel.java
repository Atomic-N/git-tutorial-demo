package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;

public class ChatChannel {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private int position;
    private int minTrustLevel;
    private int messageCount;
    private LocalDateTime lastMessageAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getMinTrustLevel() {
        return minTrustLevel;
    }

    public void setMinTrustLevel(int minTrustLevel) {
        this.minTrustLevel = minTrustLevel;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageAgo() {
        return TimeAgo.format(lastMessageAt);
    }
}
