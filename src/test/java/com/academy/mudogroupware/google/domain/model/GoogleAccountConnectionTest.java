package com.academy.mudogroupware.google.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GoogleAccountConnectionTest {

    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 14, 22);
    private static final Set<String> GRANTED_SCOPE_REQUIREMENT = Set.of("scope");

    @Test
    void connectBuildsConnectionWithSixtyDayExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "academy@mudo.co.kr", 7L, "drive.file", "refresh-token", CONNECTED_AT);

        assertThat(connection.getGoogleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(connection.getConnectedByUserId()).isEqualTo(7L);
        assertThat(connection.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(connection.getConnectedAt()).isEqualTo(CONNECTED_AT);
        assertThat(connection.getTokenExpiresAt()).isEqualTo(CONNECTED_AT.plusDays(60));
        assertThat(connection.getLastCheckedAt()).isEqualTo(CONNECTED_AT);
        assertThat(connection.isFailed()).isFalse();
    }

    @Test
    void connectThrowsWhenGoogleEmailIsBlank() {
        assertThatThrownBy(() -> GoogleAccountConnection.connect("  ", 7L, "scope", "token", CONNECTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectThrowsWhenRefreshTokenIsBlank() {
        assertThatThrownBy(() -> GoogleAccountConnection.connect("a@b.com", 7L, "scope", " ", CONNECTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deriveStatusReturnsConnectedWellBeforeExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1), GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void deriveStatusReturnsExpiringWithinThreeDaysOfExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        LocalDateTime withinWarningWindow = connection.getTokenExpiresAt().minusDays(1);

        assertThat(connection.deriveStatus(withinWarningWindow, GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.EXPIRING);
    }

    @Test
    void deriveStatusReturnsExpiredAfterExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(connection.getTokenExpiresAt().plusSeconds(1), GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.EXPIRED);
    }

    @Test
    void deriveStatusReturnsExpiringExactlyAtWarningBoundary() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(connection.getTokenExpiresAt().minusDays(3), GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.EXPIRING);
    }

    @Test
    void deriveStatusReturnsExpiredExactlyAtExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(connection.getTokenExpiresAt(), GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.EXPIRED);
    }

    @Test
    void deriveStatusReturnsFailedWhenMarkedInvalidRegardlessOfExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);
        connection.markCheckResult(CONNECTED_AT.plusDays(1), false);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1), GRANTED_SCOPE_REQUIREMENT))
                .isEqualTo(GoogleConnectionStatus.FAILED);
    }

    @Test
    void deriveStatusReturnsFailedWhenRequiredScopeIsMissing() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "openid email", "token", CONNECTED_AT);

        GoogleConnectionStatus status = connection.deriveStatus(
                CONNECTED_AT.plusDays(1), Set.of("openid", "email", "drive.file"));

        assertThat(status).isEqualTo(GoogleConnectionStatus.FAILED);
    }

    @Test
    void deriveStatusIgnoresScopeCheckWhenRequiredScopesEmpty() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "openid email", "token", CONNECTED_AT);

        GoogleConnectionStatus status = connection.deriveStatus(CONNECTED_AT.plusDays(1), Set.of());

        assertThat(status).isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void markCheckResultUpdatesLastCheckedAtAndFailedFlag() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                "a@b.com", 7L, "scope", "token", CONNECTED_AT);
        LocalDateTime checkedAt = CONNECTED_AT.plusDays(10);

        connection.markCheckResult(checkedAt, true);

        assertThat(connection.getLastCheckedAt()).isEqualTo(checkedAt);
        assertThat(connection.isFailed()).isFalse();
    }
}
