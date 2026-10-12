package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;

public class Badge {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String color;
    private int sortOrder;
    private String ruleType;
    private int ruleThreshold;
    private int awardedCount;
    private LocalDateTime awardedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public int getRuleThreshold() {
        return ruleThreshold;
    }

    public void setRuleThreshold(int ruleThreshold) {
        this.ruleThreshold = ruleThreshold;
    }

    public int getAwardedCount() {
        return awardedCount;
    }

    public void setAwardedCount(int awardedCount) {
        this.awardedCount = awardedCount;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    public boolean isEarned() {
        return awardedAt != null;
    }

    public String getAwardedAgo() {
        return TimeAgo.format(awardedAt);
    }

    public String getRuleLabel() {
        String normalized = ruleType == null || ruleType.isBlank() ? "MANUAL" : ruleType;
        return switch (normalized) {
            case "REGISTRATION" -> "完成注册自动获得";
            case "TOPICS" -> "发布话题 >= " + ruleThreshold;
            case "REPLIES" -> "发布回复 >= " + ruleThreshold;
            case "POSTS" -> "发言总数 >= " + ruleThreshold;
            case "LIKES_RECEIVED" -> "获赞数 >= " + ruleThreshold;
            case "TRUST_LEVEL" -> "信任等级 >= " + ruleThreshold;
            case "UPLOADS" -> "上传文件 >= " + ruleThreshold;
            default -> "手动授予";
        };
    }

    public boolean isAutomaticRule() {
        return ruleType != null && !ruleType.isBlank() && !"MANUAL".equals(ruleType);
    }
}
