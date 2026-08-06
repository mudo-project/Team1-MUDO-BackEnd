package com.academy.mudogroupware.google.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

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

@Entity
@Table(name = "google_account_connection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleAccountConnectionEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id")
    private Long id;

    @Column(name = "academy_id", nullable = false, unique = true)
    private Long academyId;

    @Column(name = "google_email", nullable = false, length = 255)
    private String googleEmail;

    @Column(name = "connected_by_user_id", nullable = false)
    private Long connectedByUserId;

    @Column(name = "scope", nullable = false, length = 500)
    private String scope;

    @Column(name = "encrypted_refresh_token", nullable = false, length = 1000)
    private String encryptedRefreshToken;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @Column(name = "last_checked_at", nullable = false)
    private LocalDateTime lastCheckedAt;

    @Column(name = "failed", nullable = false)
    private boolean failed;

    @Builder
    private GoogleAccountConnectionEntity(Long id, Long academyId, String googleEmail, Long connectedByUserId,
                                           String scope, String encryptedRefreshToken, LocalDateTime connectedAt,
                                           LocalDateTime tokenExpiresAt, LocalDateTime lastCheckedAt,
                                           boolean failed) {
        this.id = id;
        this.academyId = academyId;
        this.googleEmail = googleEmail;
        this.connectedByUserId = connectedByUserId;
        this.scope = scope;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.connectedAt = connectedAt;
        this.tokenExpiresAt = tokenExpiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.failed = failed;
    }

    public void updateCheckResult(LocalDateTime lastCheckedAt, boolean failed) {
        this.lastCheckedAt = lastCheckedAt;
        this.failed = failed;
    }
}
