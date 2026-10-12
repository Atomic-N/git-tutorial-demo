package com.aitef.forum.model.entity;

import java.time.LocalDateTime;

public class DirectMessageThreadMeta {
    private Long threadId;
    private Long userId;
    private boolean pinned;
    private boolean archived;
    private boolean muted;
    private LocalDateTime pinnedAt;
    private LocalDateTime archivedAt;
    private LocalDateTime mutedAt;

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(LocalDateTime pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public LocalDateTime getMutedAt() {
        return mutedAt;
    }

    public void setMutedAt(LocalDateTime mutedAt) {
        this.mutedAt = mutedAt;
    }
}
