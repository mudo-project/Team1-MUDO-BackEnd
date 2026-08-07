package com.academy.mudogroupware.google.domain.model;

import java.time.LocalDateTime;

public final class GoogleAccountConnection {

    private static final long TOKEN_VALID_DAYS = 60;
    private static final long EXPIRING_WARNING_DAYS = 3;

    private final Long id;
    private final Long academyId;
    private final String googleEmail;
    private final Long connectedByUserId;
    private final String scope;
    private final String refreshToken;
    private final LocalDateTime connectedAt;
    private final LocalDateTime tokenExpiresAt;
    private LocalDateTime lastCheckedAt;
    private boolean failed;

    private GoogleAccountConnection(Long id, Long academyId, String googleEmail, Long connectedByUserId,
                                     String scope, String refreshToken, LocalDateTime connectedAt,
                                     LocalDateTime tokenExpiresAt, LocalDateTime lastCheckedAt, boolean failed) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (googleEmail == null || googleEmail.isBlank()) {
            throw new IllegalArgumentException("googleEmail must not be null or blank");
        }
        if (connectedByUserId == null) {
            throw new IllegalArgumentException("connectedByUserId must not be null");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be null or blank");
        }
        this.id = id;
        this.academyId = academyId;
        this.googleEmail = googleEmail;
        this.connectedByUserId = connectedByUserId;
        this.scope = scope;
        this.refreshToken = refreshToken;
        this.connectedAt = connectedAt;
        this.tokenExpiresAt = tokenExpiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.failed = failed;
    }

    public static GoogleAccountConnection connect(Long academyId, String googleEmail, Long connectedByUserId,
                                                   String scope, String refreshToken, LocalDateTime connectedAt) {
        return new GoogleAccountConnection(null, academyId, googleEmail, connectedByUserId, scope, refreshToken,
                connectedAt, connectedAt.plusDays(TOKEN_VALID_DAYS), connectedAt, false);
    }

    public static GoogleAccountConnection restore(Long id, Long academyId, String googleEmail,
                                                   Long connectedByUserId, String scope, String refreshToken,
                                                   LocalDateTime connectedAt, LocalDateTime tokenExpiresAt,
                                                   LocalDateTime lastCheckedAt, boolean failed) {
        return new GoogleAccountConnection(id, academyId, googleEmail, connectedByUserId, scope, refreshToken,
                connectedAt, tokenExpiresAt, lastCheckedAt, failed);
    }

    public void markCheckResult(LocalDateTime checkedAt, boolean valid) {
        this.lastCheckedAt = checkedAt;
        this.failed = !valid;
    }

    public GoogleConnectionStatus deriveStatus(LocalDateTime now) {
        if (failed) {
            return GoogleConnectionStatus.FAILED;
        }
        if (!now.isBefore(tokenExpiresAt)) {
            return GoogleConnectionStatus.EXPIRED;
        }
        if (!now.isBefore(tokenExpiresAt.minusDays(EXPIRING_WARNING_DAYS))) {
            return GoogleConnectionStatus.EXPIRING;
        }
        return GoogleConnectionStatus.CONNECTED;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public Long getConnectedByUserId() {
        return connectedByUserId;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public boolean isFailed() {
        return failed;
    }
}
