package com.academy.mudogroupware.google.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

    @Test
    void requiredScopesReturnsOnlyDataAccessScopes() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback",
                "openid email https://www.googleapis.com/auth/drive.file", "/");

        assertThat(properties.requiredScopes())
                .containsExactly("https://www.googleapis.com/auth/drive.file");
    }

    @Test
    void requiredScopesIgnoresIdentityScopes() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "  openid   email  ", "/");

        assertThatThrownBy(properties::requiredScopes).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiredScopesDeduplicatesRepeatedDataScopes() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback",
                "openid https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.file", "/");

        assertThat(properties.requiredScopes()).isEqualTo(Set.of("https://www.googleapis.com/auth/drive.file"));
    }

    @Test
    void requiredScopesThrowsWhenScopeIsBlank() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "   ", "/");

        assertThatThrownBy(properties::requiredScopes).isInstanceOf(IllegalStateException.class);
    }
}
