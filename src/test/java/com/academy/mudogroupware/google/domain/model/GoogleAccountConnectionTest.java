package com.academy.mudogroupware.google.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GoogleAccountConnectionTest {

    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 14, 22);

    @Test
    void connectBuildsConnectionWithSixtyDayExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "academy@mudo.co.kr", 7L, "drive.file", "refresh-token", CONNECTED_AT);

        assertThat(connection.getAcademyId()).isEqualTo(1L);
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
        assertThatThrownBy(() -> GoogleAccountConnection.connect(1L, "  ", 7L, "scope", "token", CONNECTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void connectThrowsWhenRefreshTokenIsBlank() {
        assertThatThrownBy(() -> GoogleAccountConnection.connect(1L, "a@b.com", 7L, "scope", " ", CONNECTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deriveStatusReturnsConnectedWellBeforeExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1)))
                .isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void deriveStatusReturnsExpiringWithinSevenDaysOfExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        LocalDateTime withinWarningWindow = connection.getTokenExpiresAt().minusDays(3);

        assertThat(connection.deriveStatus(withinWarningWindow))
                .isEqualTo(GoogleConnectionStatus.EXPIRING);
    }

    @Test
    void deriveStatusReturnsExpiredAfterExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "a@b.com", 7L, "scope", "token", CONNECTED_AT);

        assertThat(connection.deriveStatus(connection.getTokenExpiresAt().plusSeconds(1)))
                .isEqualTo(GoogleConnectionStatus.EXPIRED);
    }

    @Test
    void deriveStatusReturnsFailedWhenMarkedInvalidRegardlessOfExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "a@b.com", 7L, "scope", "token", CONNECTED_AT);
        connection.markCheckResult(CONNECTED_AT.plusDays(1), false);

        assertThat(connection.deriveStatus(CONNECTED_AT.plusDays(1)))
                .isEqualTo(GoogleConnectionStatus.FAILED);
    }

    @Test
    void markCheckResultUpdatesLastCheckedAtAndFailedFlag() {
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                1L, "a@b.com", 7L, "scope", "token", CONNECTED_AT);
        LocalDateTime checkedAt = CONNECTED_AT.plusDays(10);

        connection.markCheckResult(checkedAt, true);

        assertThat(connection.getLastCheckedAt()).isEqualTo(checkedAt);
        assertThat(connection.isFailed()).isFalse();
    }
}
