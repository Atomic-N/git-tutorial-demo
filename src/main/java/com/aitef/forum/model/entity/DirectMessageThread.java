package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;

public class DirectMessageThread {
    private Long id;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long partnerId;
    private String partnerUsername;
    private String partnerDisplayName;
    private String partnerAvatarUrl;
    private String latestPreview;
    private LocalDateTime latestMessageAt;
    private boolean unread;
    private int unreadCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getPartnerDisplayName() {
        return partnerDisplayName;
    }

    public void setPartnerDisplayName(String partnerDisplayName) {
        this.partnerDisplayName = partnerDisplayName;
    }

    public String getPartnerAvatarUrl() {
        return partnerAvatarUrl;
    }

    public void setPartnerAvatarUrl(String partnerAvatarUrl) {
        this.partnerAvatarUrl = partnerAvatarUrl;
    }

    public String getLatestPreview() {
        return latestPreview;
    }

    public void setLatestPreview(String latestPreview) {
        this.latestPreview = latestPreview;
    }

    public LocalDateTime getLatestMessageAt() {
        return latestMessageAt;
    }

    public void setLatestMessageAt(LocalDateTime latestMessageAt) {
        this.latestMessageAt = latestMessageAt;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLatestAgo() {
        return TimeAgo.format(latestMessageAt == null ? updatedAt : latestMessageAt);
    }
}
