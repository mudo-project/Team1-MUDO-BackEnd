package com.academy.mudogroupware.notice.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;

public final class Notice {

    private final Long id;
    private final Long authorUserId;
    private String title;
    private String content;
    private boolean pinned;
    private long viewCount;
    private final List<NoticeAttachment> attachments;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime retentionUntil;

    private Notice(Long id, Long authorUserId, String title, String content, boolean pinned,
                    long viewCount, List<NoticeAttachment> attachments, LocalDateTime createdAt,
                    LocalDateTime updatedAt, LocalDateTime deletedAt, LocalDateTime retentionUntil) {
        if (authorUserId == null) {
            throw new IllegalArgumentException("authorUserId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new NoticeException(NoticeErrorCode.TITLE_REQUIRED);
        }
        if (content == null || content.isBlank()) {
            throw new NoticeException(NoticeErrorCode.CONTENT_REQUIRED);
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        this.id = id;
        this.authorUserId = authorUserId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.viewCount = viewCount;
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.retentionUntil = retentionUntil;
    }

    public static Notice create(Long authorUserId, String title, String content,
                                 boolean pinned, List<NoticeAttachment> attachments, LocalDateTime now) {
        return new Notice(null, authorUserId, title, content, pinned, 0L, attachments, now, now,
                null, null);
    }

    public static Notice restore(Long id, Long authorUserId, String title, String content,
                                  boolean pinned, long viewCount, List<NoticeAttachment> attachments,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Notice(id, authorUserId, title, content, pinned, viewCount, attachments, createdAt,
                updatedAt, null, null);
    }

    public static Notice restore(Long id, Long authorUserId, String title, String content,
                                  boolean pinned, long viewCount, List<NoticeAttachment> attachments,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                                  LocalDateTime retentionUntil) {
        return new Notice(id, authorUserId, title, content, pinned, viewCount, attachments, createdAt,
                updatedAt, deletedAt, retentionUntil);
    }

    public void recordView() {
        this.viewCount++;
    }

    public void update(String title, String content, LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new NoticeException(NoticeErrorCode.TITLE_REQUIRED);
        }
        if (content == null || content.isBlank()) {
            throw new NoticeException(NoticeErrorCode.CONTENT_REQUIRED);
        }
        this.title = title;
        this.content = content;
        this.updatedAt = now;
    }

    public void pin() {
        this.pinned = true;
    }

    public void unpin() {
        this.pinned = false;
    }

    public boolean isAuthor(Long userId) {
        return authorUserId.equals(userId);
    }

    public void markDeleted(LocalDateTime deletedAt, LocalDateTime retentionUntil) {
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        if (retentionUntil == null) {
            throw new IllegalArgumentException("retentionUntil must not be null");
        }
        this.deletedAt = deletedAt;
        this.retentionUntil = retentionUntil;
        this.updatedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public long getViewCount() {
        return viewCount;
    }

    public List<NoticeAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getRetentionUntil() {
        return retentionUntil;
    }
}
