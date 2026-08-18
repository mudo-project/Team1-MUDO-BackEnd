package com.academy.mudogroupware.notification.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.academy.mudogroupware.global.infrastructure.persistence.SoftDeleteTimeEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notification",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notification_idempotency_key",
                        columnNames = {"idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends SoftDeleteTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 250)
    private String message;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private NotificationEntity(Long recipientUserId, String type, Long targetId, String message,
                               String idempotencyKey) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.targetId = targetId;
        this.message = message;
        this.idempotencyKey = idempotencyKey;
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
}
