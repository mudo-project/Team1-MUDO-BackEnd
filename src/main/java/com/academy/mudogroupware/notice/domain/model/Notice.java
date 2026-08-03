package com.academy.mudogroupware.notice.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public final class Notice {

    private final Long id;
    private final Long academyId;
    private final Long authorUserId;
    private String title;
    private String content;
    private boolean pinned;
    private long viewCount;
    private final List<NoticeAttachment> attachments;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Notice(Long id, Long academyId, Long authorUserId, String title, String content, boolean pinned,
                    long viewCount, List<NoticeAttachment> attachments, LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (authorUserId == null) {
            throw new IllegalArgumentException("authorUserId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("공지 제목은 비어 있을 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("공지 내용은 비어 있을 수 없습니다.");
        }
        this.id = id;
        this.academyId = academyId;
        this.authorUserId = authorUserId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.viewCount = viewCount;
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Notice create(Long academyId, Long authorUserId, String title, String content,
                                 boolean pinned, List<NoticeAttachment> attachments) {
        LocalDateTime now = LocalDateTime.now();
        return new Notice(null, academyId, authorUserId, title, content, pinned, 0L, attachments, now, now);
    }

    public static Notice restore(Long id, Long academyId, Long authorUserId, String title, String content,
                                  boolean pinned, long viewCount, List<NoticeAttachment> attachments,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Notice(id, academyId, authorUserId, title, content, pinned, viewCount, attachments, createdAt,
                updatedAt);
    }

    public void recordView() {
        this.viewCount++;
    }

    public void update(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("공지 제목은 비어 있을 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("공지 내용은 비어 있을 수 없습니다.");
        }
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
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

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
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
}
