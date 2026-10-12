package com.aitef.forum.model.dto;

public class SearchCriteria {
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private String author;
    private boolean hideReadTopics;
    private boolean solvedOnly;
    private int viewerTrustLevel;
    private Integer createdWithinDays;
    private Integer minReplies;
    private Integer maxReplies;
    private Integer minViews;
    private Integer maxViews;
    private String orderBy = "LATEST";
    private int page = 1;
    private int size = 20;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isHideReadTopics() {
        return hideReadTopics;
    }

    public void setHideReadTopics(boolean hideReadTopics) {
        this.hideReadTopics = hideReadTopics;
    }

    public boolean isSolvedOnly() {
        return solvedOnly;
    }

    public void setSolvedOnly(boolean solvedOnly) {
        this.solvedOnly = solvedOnly;
    }

    public int getViewerTrustLevel() {
        return viewerTrustLevel;
    }

    public void setViewerTrustLevel(int viewerTrustLevel) {
        this.viewerTrustLevel = viewerTrustLevel;
    }

    public Integer getCreatedWithinDays() {
        return createdWithinDays;
    }

    public void setCreatedWithinDays(Integer createdWithinDays) {
        this.createdWithinDays = createdWithinDays;
    }

    public Integer getMinReplies() {
        return minReplies;
    }

    public void setMinReplies(Integer minReplies) {
        this.minReplies = minReplies;
    }

    public Integer getMaxReplies() {
        return maxReplies;
    }

    public void setMaxReplies(Integer maxReplies) {
        this.maxReplies = maxReplies;
    }

    public Integer getMinViews() {
        return minViews;
    }

    public void setMinViews(Integer minViews) {
        this.minViews = minViews;
    }

    public Integer getMaxViews() {
        return maxViews;
    }

    public void setMaxViews(Integer maxViews) {
        this.maxViews = maxViews;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
