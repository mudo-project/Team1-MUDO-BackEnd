package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;

public final class PushSubscription {

    private final Long id;
    private final Long userId;
    private final String endpoint;
    private final String p256dh;
    private final String auth;
    private final LocalDateTime createdAt;

    private PushSubscription(Long id, Long userId, String endpoint, String p256dh, String auth,
                              LocalDateTime createdAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint must not be blank");
        }
        if (p256dh == null || p256dh.isBlank()) {
            throw new IllegalArgumentException("p256dh must not be blank");
        }
        if (auth == null || auth.isBlank()) {
            throw new IllegalArgumentException("auth must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }

    public static PushSubscription create(Long userId, String endpoint, String p256dh, String auth,
                                           LocalDateTime now) {
        return new PushSubscription(null, userId, endpoint, p256dh, auth, now);
    }

    public static PushSubscription restore(Long id, Long userId, String endpoint, String p256dh, String auth,
                                            LocalDateTime createdAt) {
        return new PushSubscription(id, userId, endpoint, p256dh, auth, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
