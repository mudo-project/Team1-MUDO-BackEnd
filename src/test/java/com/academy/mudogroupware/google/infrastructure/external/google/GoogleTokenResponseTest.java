package com.academy.mudogroupware.google.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleTokenResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsOptionalRefreshTokenExpirationFromGoogleResponse() throws Exception {
        GoogleTokenResponse response = objectMapper.readValue("""
                {
                  "access_token": "access-token",
                  "refresh_token": "refresh-token",
                  "scope": "https://www.googleapis.com/auth/drive.file",
                  "refresh_token_expires_in": 604800
                }
                """, GoogleTokenResponse.class);

        assertThat(response.refreshTokenExpiresInSeconds()).isEqualTo(604800L);
    }

    @Test
    void keepsRefreshTokenExpirationNullWhenGoogleDoesNotProvideIt() throws Exception {
        GoogleTokenResponse response = objectMapper.readValue("""
                {
                  "access_token": "access-token",
                  "refresh_token": "refresh-token",
                  "scope": "https://www.googleapis.com/auth/drive.file"
                }
                """, GoogleTokenResponse.class);

        assertThat(response.refreshTokenExpiresInSeconds()).isNull();
    }
}
