package com.academy.mudogroupware.google.domain.model;

import java.time.LocalDateTime;
import java.util.Set;
public final class GoogleAccountConnection {

    private static final long EXPIRING_WARNING_DAYS = 3;

    private final Long id;
    private final String googleEmail;
    private final Long connectedByUserId;
    private final String scope;
    private final String refreshToken;
    private final LocalDateTime connectedAt;
    private final LocalDateTime refreshTokenExpiresAt;
    private LocalDateTime lastCheckedAt;
    private boolean failed;

    private GoogleAccountConnection(Long id, String googleEmail, Long connectedByUserId,
                                     String scope, String refreshToken, LocalDateTime connectedAt,
                                     LocalDateTime refreshTokenExpiresAt, LocalDateTime lastCheckedAt, boolean failed) {
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
        this.googleEmail = googleEmail;
        this.connectedByUserId = connectedByUserId;
        this.scope = scope;
        this.refreshToken = refreshToken;
        this.connectedAt = connectedAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.lastCheckedAt = lastCheckedAt;
        this.failed = failed;
    }

    public static GoogleAccountConnection connect(String googleEmail, Long connectedByUserId,
                                                   String scope, String refreshToken, LocalDateTime connectedAt,
                                                   LocalDateTime refreshTokenExpiresAt) {
        return new GoogleAccountConnection(null, googleEmail, connectedByUserId, scope, refreshToken,
                connectedAt, refreshTokenExpiresAt, connectedAt, false);
    }

    public static GoogleAccountConnection restore(Long id, String googleEmail,
                                                   Long connectedByUserId, String scope, String refreshToken,
                                                   LocalDateTime connectedAt, LocalDateTime refreshTokenExpiresAt,
                                                   LocalDateTime lastCheckedAt, boolean failed) {
        return new GoogleAccountConnection(id, googleEmail, connectedByUserId, scope, refreshToken,
                connectedAt, refreshTokenExpiresAt, lastCheckedAt, failed);
    }

    public void markCheckResult(LocalDateTime checkedAt, boolean valid) {
        this.lastCheckedAt = checkedAt;
        this.failed = !valid;
    }

    public void markCheckAttempted(LocalDateTime checkedAt) {
        this.lastCheckedAt = checkedAt;
    }

    public GoogleConnectionStatus deriveStatus(LocalDateTime now) {
        if (failed) {
            return GoogleConnectionStatus.FAILED;
        }
        if (refreshTokenExpiresAt == null) {
            return GoogleConnectionStatus.CONNECTED;
        }
        if (!now.isBefore(refreshTokenExpiresAt)) {
            return GoogleConnectionStatus.EXPIRED;
        }
        if (!now.isBefore(refreshTokenExpiresAt.minusDays(EXPIRING_WARNING_DAYS))) {
            return GoogleConnectionStatus.EXPIRING;
        }
        return GoogleConnectionStatus.CONNECTED;
    }

    public boolean hasAllScopes(Set<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true;
        }
        Set<String> grantedScopes = scope == null || scope.isBlank()
                ? Set.of()
                : Set.of(scope.trim().split("\\s+"));
        return grantedScopes.containsAll(requiredScopes);
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public boolean isFailed() {
        return failed;
    }
}
