package com.academy.mudogroupware.google.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

    @Test
    void requiredScopesSplitsSpaceSeparatedScopeString() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback",
                "openid email https://www.googleapis.com/auth/drive.file", "/");

        assertThat(properties.requiredScopes()).containsExactlyInAnyOrder(
                "openid", "email", "https://www.googleapis.com/auth/drive.file");
    }

    @Test
    void requiredScopesTrimsExtraWhitespace() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "  openid   email  ", "/");

        assertThat(properties.requiredScopes()).isEqualTo(Set.of("openid", "email"));
    }

    @Test
    void requiredScopesDeduplicatesRepeatedScopes() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "openid openid email", "/");

        assertThat(properties.requiredScopes()).isEqualTo(Set.of("openid", "email"));
    }

    @Test
    void requiredScopesThrowsWhenScopeIsBlank() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "   ", "/");

        assertThatThrownBy(properties::requiredScopes).isInstanceOf(IllegalStateException.class);
    }
}
