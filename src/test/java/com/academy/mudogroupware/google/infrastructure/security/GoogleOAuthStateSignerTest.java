package com.academy.mudogroupware.google.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthStateInvalidException;

class GoogleOAuthStateSignerTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");

    @Test
    void verifyRestoresOriginalClaimsAfterSign() {
        GoogleOAuthStateSigner signer = new GoogleOAuthStateSigner(fixedClock(), "test-secret");
        GoogleOAuthStateClaims claims = new GoogleOAuthStateClaims(1L, 7L, true);

        String state = signer.sign(claims);
        GoogleOAuthStateClaims restored = signer.verify(state);

        assertThat(restored).isEqualTo(claims);
    }

    @Test
    void verifyThrowsWhenStateIsTampered() {
        GoogleOAuthStateSigner signer = new GoogleOAuthStateSigner(fixedClock(), "test-secret");
        String state = signer.sign(new GoogleOAuthStateClaims(1L, 7L, false));
        String tampered = state.replaceFirst("^1", "2");

        assertThatThrownBy(() -> signer.verify(tampered))
                .isInstanceOf(GoogleOAuthStateInvalidException.class);
    }

    @Test
    void verifyThrowsWhenStateIsExpired() {
        GoogleOAuthStateSigner signer = new GoogleOAuthStateSigner(fixedClock(), "test-secret");
        String state = signer.sign(new GoogleOAuthStateClaims(1L, 7L, false));

        GoogleOAuthStateSigner futureSigner =
                new GoogleOAuthStateSigner(Clock.fixed(NOW.plusSeconds(1000), ZoneOffset.UTC), "test-secret");

        assertThatThrownBy(() -> futureSigner.verify(state))
                .isInstanceOf(GoogleOAuthStateInvalidException.class);
    }

    @Test
    void verifyThrowsWhenStateIsMalformed() {
        GoogleOAuthStateSigner signer = new GoogleOAuthStateSigner(fixedClock(), "test-secret");

        assertThatThrownBy(() -> signer.verify("not-a-valid-state"))
                .isInstanceOf(GoogleOAuthStateInvalidException.class);
    }

    @Test
    void constructorThrowsWhenJwtSecretIsBlank() {
        assertThatThrownBy(() -> new GoogleOAuthStateSigner(fixedClock(), "  "))
                .isInstanceOf(IllegalStateException.class);
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
