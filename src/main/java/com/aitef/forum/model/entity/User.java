package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String location;
    private String website;
    private int trustLevel;
    private String role;
    private String status;
    private int topicsCount;
    private int postsCount;
    private int likesReceivedCount;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> roleCodes = new LinkedHashSet<>();
    private Set<Long> moderatedCategoryIds = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public int getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(int trustLevel) {
        this.trustLevel = trustLevel;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTopicsCount() {
        return topicsCount;
    }

    public void setTopicsCount(int topicsCount) {
        this.topicsCount = topicsCount;
    }

    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = postsCount;
    }

    public int getLikesReceivedCount() {
        return likesReceivedCount;
    }

    public void setLikesReceivedCount(int likesReceivedCount) {
        this.likesReceivedCount = likesReceivedCount;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
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

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(Set<String> roleCodes) {
        this.roleCodes = roleCodes == null ? new LinkedHashSet<>() : roleCodes;
    }

    public Set<Long> getModeratedCategoryIds() {
        return moderatedCategoryIds;
    }

    public void setModeratedCategoryIds(Set<Long> moderatedCategoryIds) {
        this.moderatedCategoryIds = moderatedCategoryIds == null ? new LinkedHashSet<>() : moderatedCategoryIds;
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isModerator() {
        return hasRole("MODERATOR") || isAdmin();
    }

    public boolean isScopedModerator() {
        return hasRole("MODERATOR");
    }

    public boolean moderatesCategory(Long categoryId) {
        return categoryId != null && moderatedCategoryIds.contains(categoryId);
    }

    public boolean canModerateCategory(Long categoryId) {
        return isAdmin() || (isScopedModerator() && moderatesCategory(categoryId));
    }

    public boolean hasRole(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (role != null && code.equalsIgnoreCase(role)) {
            return true;
        }
        return roleCodes.stream().anyMatch(existing -> code.equalsIgnoreCase(existing));
    }

    public String getJoinedAgo() {
        return TimeAgo.format(createdAt);
    }
}
