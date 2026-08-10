package com.academy.mudogroupware.rollcall.domain.model;

import java.time.LocalDateTime;

public final class MessageTemplate {

    private final Long id;
    private final Long academyId;
    private String name;
    private final AttendanceStatus status;
    private String content;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MessageTemplate(Long id, Long academyId, String name, AttendanceStatus status, String content,
                             Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or blank");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.status = status;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MessageTemplate create(Long academyId, String name, AttendanceStatus status, String content,
                                          Long createdBy, LocalDateTime now) {
        return new MessageTemplate(null, academyId, name, status, content, createdBy, now, now);
    }

    public static MessageTemplate restore(Long id, Long academyId, String name, AttendanceStatus status,
                                           String content, Long createdBy, LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
        return new MessageTemplate(id, academyId, name, status, content, createdBy, createdAt, updatedAt);
    }

    public void update(String name, String content, LocalDateTime now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or blank");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.name = name;
        this.content = content;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getName() {
        return name;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
