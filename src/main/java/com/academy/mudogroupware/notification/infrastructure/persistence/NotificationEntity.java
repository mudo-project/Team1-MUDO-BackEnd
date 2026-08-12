package com.academy.mudogroupware.notification.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.academy.mudogroupware.global.infrastructure.persistence.SoftDeleteTimeEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
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

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private NotificationEntity(Long recipientUserId, String type, Long targetId, String message) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.targetId = targetId;
        this.message = message;
    }

    // 이미 읽은 알림에 다시 호출해도 최초 읽은 시각을 유지한다(멱등).
    public void markAsRead(LocalDateTime readAt) {
        if (this.readAt != null) {
            return;
        }
        this.readAt = readAt;
    }
}
