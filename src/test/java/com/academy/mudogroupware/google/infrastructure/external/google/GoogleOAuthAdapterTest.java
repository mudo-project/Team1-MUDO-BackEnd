package com.academy.mudogroupware.google.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleOAuthAdapterTest {

    private final GoogleOAuthAdapter adapter = new GoogleOAuthAdapter(
            mock(RestClient.class),
            new GoogleOAuthProperties("client-id", "client-secret", "https://example.com/callback",
                    "openid email drive.file", "/"),
            new ObjectMapper());

    @Test
    void isTokenRevokedReturnsTrueForInvalidGrant() {
        HttpClientErrorException e = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertThat(adapter.isTokenRevoked(e)).isTrue();
    }

    @Test
    void isTokenRevokedReturnsFalseForOtherBadRequestError() {
        HttpClientErrorException e = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"error\":\"invalid_client\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        assertThat(adapter.isTokenRevoked(e)).isFalse();
    }

    @Test
    void isTokenRevokedReturnsFalseForNonBadRequestStatus() {
        HttpClientErrorException e = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);

        assertThat(adapter.isTokenRevoked(e)).isFalse();
    }

    @Test
    void isTokenRevokedReturnsFalseForUnparseableBody() {
        HttpClientErrorException e = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "not json".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(adapter.isTokenRevoked(e)).isFalse();
    }
}
