package com.aitef.forum.model.entity;

import com.aitef.forum.util.TimeAgo;

import java.time.LocalDateTime;

public class DirectMessage {
    private Long id;
    private Long threadId;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private Long quotedMessageId;
    private String quotedMessageBody;
    private String quotedSenderName;
    private String bodyRaw;
    private String bodyCooked;
    private String status;
    private String messageStatus;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime deliveredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public Long getQuotedMessageId() {
        return quotedMessageId;
    }

    public void setQuotedMessageId(Long quotedMessageId) {
        this.quotedMessageId = quotedMessageId;
    }

    public String getQuotedMessageBody() {
        return quotedMessageBody;
    }

    public void setQuotedMessageBody(String quotedMessageBody) {
        this.quotedMessageBody = quotedMessageBody;
    }

    public String getQuotedSenderName() {
        return quotedSenderName;
    }

    public void setQuotedSenderName(String quotedSenderName) {
        this.quotedSenderName = quotedSenderName;
    }

    public String getBodyRaw() {
        return bodyRaw;
    }

    public void setBodyRaw(String bodyRaw) {
        this.bodyRaw = bodyRaw;
    }

    public String getBodyCooked() {
        return bodyCooked;
    }

    public void setBodyCooked(String bodyCooked) {
        this.bodyCooked = bodyCooked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getCreatedAgo() {
        return TimeAgo.format(createdAt);
    }

    /**
     * 获取消息状态图标
     */
    public String getStatusIcon() {
        if (messageStatus == null) {
            return "";
        }
        switch (messageStatus) {
            case "READ":
                return "✓✓";
            case "DELIVERED":
            case "SENT":
                return "✓";
            case "SENDING":
                return "⏱";
            default:
                return "";
        }
    }

    /**
     * 判断消息是否已读
     */
    public boolean isRead() {
        return "READ".equals(messageStatus);
    }
}
