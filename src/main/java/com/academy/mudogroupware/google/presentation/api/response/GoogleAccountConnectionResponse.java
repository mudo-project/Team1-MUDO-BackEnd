package com.academy.mudogroupware.google.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.google.application.query.GoogleAccountConnectionView;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;

public record GoogleAccountConnectionResponse(String googleEmail, Long connectedByUserId, String scope,
                                               LocalDateTime connectedAt, LocalDateTime refreshTokenExpiresAt,
                                               LocalDateTime lastCheckedAt, GoogleConnectionStatus status) {

    public static GoogleAccountConnectionResponse from(GoogleAccountConnectionView view) {
        return new GoogleAccountConnectionResponse(view.googleEmail(), view.connectedByUserId(), view.scope(),
                view.connectedAt(), view.refreshTokenExpiresAt(), view.lastCheckedAt(), view.status());
    }
}
