package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String endpoint;

    @Setter
    @Column(nullable = false, length = 255)
    private String p256dh;

    @Setter
    @Column(nullable = false, length = 255)
    private String auth;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PushSubscriptionEntity(Long id, Long userId, String endpoint, String p256dh, String auth,
                                    LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }
}
