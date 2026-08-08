package com.academy.mudogroupware.google.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

    @Test
    void scopeSetSplitsSpaceSeparatedScopeString() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback",
                "openid email https://www.googleapis.com/auth/drive.file", "/");

        assertThat(properties.scopeSet()).containsExactlyInAnyOrder(
                "openid", "email", "https://www.googleapis.com/auth/drive.file");
    }

    @Test
    void scopeSetTrimsExtraWhitespace() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "  openid   email  ", "/");

        assertThat(properties.scopeSet()).isEqualTo(Set.of("openid", "email"));
    }
}
