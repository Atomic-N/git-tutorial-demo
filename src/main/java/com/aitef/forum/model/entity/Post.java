package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;

public class Post {
    private Long id;
    private Long topicId;
    private Long userId;
    private Long replyToPostId;
    private int postNumber;
    private String rawContent;
    private String cookedContent;
    private int likesCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String authorRole;
    private String replyToAuthorUsername;
    private String replyToAuthorDisplayName;
    private Integer replyToPostNumber;
    private String replyToExcerpt;
    private boolean likedByCurrentUser;
    private boolean solution;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getReplyToPostId() {
        return replyToPostId;
    }

    public void setReplyToPostId(Long replyToPostId) {
        this.replyToPostId = replyToPostId;
    }

    public int getPostNumber() {
        return postNumber;
    }

    public void setPostNumber(int postNumber) {
        this.postNumber = postNumber;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getCookedContent() {
        return cookedContent;
    }

    public void setCookedContent(String cookedContent) {
        this.cookedContent = cookedContent;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public boolean isAuthorAdmin() {
        return "ADMIN".equalsIgnoreCase(authorRole);
    }

    public boolean isAuthorModerator() {
        return "MODERATOR".equalsIgnoreCase(authorRole);
    }

    public String getReplyToAuthorUsername() {
        return replyToAuthorUsername;
    }

    public void setReplyToAuthorUsername(String replyToAuthorUsername) {
        this.replyToAuthorUsername = replyToAuthorUsername;
    }

    public String getReplyToAuthorDisplayName() {
        return replyToAuthorDisplayName;
    }

    public void setReplyToAuthorDisplayName(String replyToAuthorDisplayName) {
        this.replyToAuthorDisplayName = replyToAuthorDisplayName;
    }

    public Integer getReplyToPostNumber() {
        return replyToPostNumber;
    }

    public void setReplyToPostNumber(Integer replyToPostNumber) {
        this.replyToPostNumber = replyToPostNumber;
    }

    public String getReplyToExcerpt() {
        return replyToExcerpt;
    }

    public void setReplyToExcerpt(String replyToExcerpt) {
        this.replyToExcerpt = replyToExcerpt;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public boolean isSolution() {
        return solution;
    }

    public void setSolution(boolean solution) {
        this.solution = solution;
    }

    public String getCreatedAgo() {
        return TimeAgo.format(createdAt);
    }
}
