package com.academy.mudogroupware.google.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;

public record GoogleAccountConnectionView(String googleEmail, Long connectedByUserId, String scope,
                                           LocalDateTime connectedAt, LocalDateTime tokenExpiresAt,
                                           LocalDateTime lastCheckedAt, GoogleConnectionStatus status) {
}
