package com.academy.mudogroupware.google.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GoogleAccountConnectionTest {

    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 14, 22);

    @Test
    void connectBuildsConnectionWithoutExpiryWhenGoogleDoesNotProvideOne() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "academy@mudo.co.kr", 7L, "drive.file", "refresh-token", CONNECTED_AT, null);

        assertThat(connection.getGoogleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(connection.getConnectedByUserId()).isEqualTo(7L);
        assertThat(connection.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(connection.getConnectedAt()).isEqualTo(CONNECTED_AT);
        assertThat(connection.getRefreshTokenExpiresAt()).isNull();
        assertThat(connection.getLastCheckedAt()).isEqualTo(CONNECTED_AT);
        assertThat(connection.isFailed()).isFalse();
    }

    @Test
    void connectThrowsWhenGoogleEmailIsBlank() {
        assertThatThrownBy(() -> GoogleAccountConnection.connect(
                "  ", 7L, "scope", "token", CONNECTED_AT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectThrowsWhenRefreshTokenIsBlank() {
        assertThatThrownBy(() -> GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", " ", CONNECTED_AT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deriveStatusReturnsConnectedWhenGoogleDidNotProvideRefreshTokenExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, null);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(365)))
                .isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void deriveStatusReturnsExpiringAtThreeDayBoundaryBeforeActualExpiry() {
        LocalDateTime refreshTokenExpiresAt = CONNECTED_AT.plusDays(30);
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, refreshTokenExpiresAt);

        assertThat(connection.deriveStatus(refreshTokenExpiresAt.minusDays(3)))
                .isEqualTo(GoogleConnectionStatus.EXPIRING);
        assertThat(connection.deriveStatus(refreshTokenExpiresAt.minusDays(3).minusNanos(1)))
                .isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void deriveStatusReturnsExpiredAtActualExpiry() {
        LocalDateTime refreshTokenExpiresAt = CONNECTED_AT.plusDays(30);
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, refreshTokenExpiresAt);

        assertThat(connection.deriveStatus(refreshTokenExpiresAt))
                .isEqualTo(GoogleConnectionStatus.EXPIRED);
    }

    @Test
    void deriveStatusReturnsFailedWhenMarkedInvalidRegardlessOfExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, null);
        connection.markCheckResult(CONNECTED_AT.plusDays(1), false);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1)))
                .isEqualTo(GoogleConnectionStatus.FAILED);
    }

    @Test
    void deriveStatusDoesNotUseGrantedScopeAsConnectionFailure() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "openid email", "token", CONNECTED_AT, null);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1)))
                .isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void markCheckResultUpdatesLastCheckedAtAndFailedFlag() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, null);
        LocalDateTime checkedAt = CONNECTED_AT.plusDays(10);

        connection.markCheckResult(checkedAt, true);

        assertThat(connection.getLastCheckedAt()).isEqualTo(checkedAt);
        assertThat(connection.isFailed()).isFalse();
    }

    @Test
    void markCheckAttemptedUpdatesLastCheckedAtWithoutChangingFailedFlag() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT, null);
        connection.markCheckResult(CONNECTED_AT.plusDays(1), false);
        LocalDateTime attemptedAt = CONNECTED_AT.plusDays(2);

        connection.markCheckAttempted(attemptedAt);

        assertThat(connection.getLastCheckedAt()).isEqualTo(attemptedAt);
        assertThat(connection.isFailed()).isTrue();
    }
}
