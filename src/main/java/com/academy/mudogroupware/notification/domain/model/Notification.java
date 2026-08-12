package com.academy.mudogroupware.notification.domain.model;

import java.time.LocalDateTime;

public final class Notification {

    public static final int MAX_MESSAGE_LENGTH = 250;

    private final Long id;
    private final Long recipientUserId;
    private final String type;
    private final Long targetId;
    private final String message;
    private LocalDateTime readAt;
    private final LocalDateTime createdAt;

    private Notification(Long id, Long recipientUserId, String type, Long targetId, String message,
                          LocalDateTime readAt, LocalDateTime createdAt) {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("recipientUserId must not be null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("targetId must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message must not exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.targetId = targetId;
        this.message = message;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public static Notification create(Long recipientUserId, String type, Long targetId, String message) {
        return new Notification(null, recipientUserId, type, targetId, message, null, null);
    }

    public static Notification restore(Long id, Long recipientUserId, String type, Long targetId, String message,
                                        LocalDateTime readAt, LocalDateTime createdAt) {
        return new Notification(id, recipientUserId, type, targetId, message, readAt, createdAt);
    }

    // 이미 읽은 알림에 다시 호출해도 최초 읽은 시각을 유지한다(멱등).
    public void markAsRead(LocalDateTime readAt) {
        if (readAt == null) {
            throw new IllegalArgumentException("readAt must not be null");
        }
        if (this.readAt != null) {
            return;
        }
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public String getType() {
        return type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
