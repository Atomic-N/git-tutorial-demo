package com.aitef.forum.model.dto;

import com.aitef.forum.model.entity.Tag;
import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TopicCard {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String slug;
    private String excerpt;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String categoryName;
    private String categorySlug;
    private String categoryColor;
    private String categoryIcon;
    private int viewsCount;
    private int repliesCount;
    private int likesCount;
    private int bookmarksCount;
    private boolean pinned;
    private boolean solved;
    private Long solutionPostId;
    private boolean likedByCurrentUser;
    private boolean bookmarkedByCurrentUser;
    private boolean unreadByCurrentUser;
    private int unreadPostsCount;
    private double hotScore;
    private LocalDateTime createdAt;
    private LocalDateTime lastPostAt;
    private List<Tag> tags = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public int getRepliesCount() {
        return repliesCount;
    }

    public void setRepliesCount(int repliesCount) {
        this.repliesCount = repliesCount;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public int getBookmarksCount() {
        return bookmarksCount;
    }

    public void setBookmarksCount(int bookmarksCount) {
        this.bookmarksCount = bookmarksCount;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public Long getSolutionPostId() {
        return solutionPostId;
    }

    public void setSolutionPostId(Long solutionPostId) {
        this.solutionPostId = solutionPostId;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public boolean isBookmarkedByCurrentUser() {
        return bookmarkedByCurrentUser;
    }

    public void setBookmarkedByCurrentUser(boolean bookmarkedByCurrentUser) {
        this.bookmarkedByCurrentUser = bookmarkedByCurrentUser;
    }

    public boolean isUnreadByCurrentUser() {
        return unreadByCurrentUser;
    }

    public void setUnreadByCurrentUser(boolean unreadByCurrentUser) {
        this.unreadByCurrentUser = unreadByCurrentUser;
    }

    public int getUnreadPostsCount() {
        return unreadPostsCount;
    }

    public void setUnreadPostsCount(int unreadPostsCount) {
        this.unreadPostsCount = unreadPostsCount;
    }

    public double getHotScore() {
        return hotScore;
    }

    public void setHotScore(double hotScore) {
        this.hotScore = hotScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastPostAt() {
        return lastPostAt;
    }

    public void setLastPostAt(LocalDateTime lastPostAt) {
        this.lastPostAt = lastPostAt;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getCreatedAgo() {
        return TimeAgo.format(createdAt);
    }

    public String getLastPostAgo() {
        return TimeAgo.format(lastPostAt);
    }
}
