package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountConnectionInvalidException;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;
import com.academy.mudogroupware.google.infrastructure.external.google.GoogleOAuthProperties;

@ExtendWith(MockitoExtension.class)
class GetGoogleAccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private GoogleOAuthPort googleOAuthPort;

    private GetGoogleAccessTokenService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "openid email drive.file", "/");
        service = new GetGoogleAccessTokenService(
                googleAccountConnectionRepository, googleOAuthPort, clock, properties);
    }

    @Test
    void getAccessTokenThrowsWhenNotConnected() {
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(GoogleAccountNotConnectedException.class);
        verifyNoInteractions(googleOAuthPort);
    }

    @Test
    void getAccessTokenThrowsWithoutCallingGoogleWhenScopeInsufficient() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email", "refresh-token", CONNECTED_AT,
                CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(GoogleAccountConnectionInvalidException.class);
        verifyNoInteractions(googleOAuthPort);
    }

    @Test
    void getAccessTokenThrowsWithoutCallingGoogleWhenExpired() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.minusDays(1), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(GoogleAccountConnectionInvalidException.class);
        verify(googleOAuthPort, never()).refreshAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getAccessTokenReturnsFreshTokenWhenConnectionValid() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult("new-access-token", "refresh-token", "openid email drive.file"));

        String accessToken = service.getAccessToken();

        assertThat(accessToken).isEqualTo("new-access-token");
    }

    @Test
    void getAccessTokenWrapsGoogleOAuthCallExceptionWhenRefreshFails() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenThrow(new GoogleOAuthCallException("구글 토큰 발급에 실패했습니다."));

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(GoogleOAuthFailedException.class);
    }
}
