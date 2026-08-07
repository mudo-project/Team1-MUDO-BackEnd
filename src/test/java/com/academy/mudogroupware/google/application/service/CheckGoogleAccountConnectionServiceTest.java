package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.google.application.command.CheckGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class CheckGoogleAccountConnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private GoogleOAuthPort googleOAuthPort;

    private CheckGoogleAccountConnectionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CheckGoogleAccountConnectionService(googleAccountConnectionRepository, googleOAuthPort, clock);
    }

    @Test
    void checkMarksValidWhenGoogleRefreshSucceeds() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, 1L, "a@b.com", 7L, "scope", "refresh-token", CONNECTED_AT, CONNECTED_AT.plusDays(60),
                CONNECTED_AT, false);
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", null, "scope"));

        service.check(new CheckGoogleConnectionCommand(1L));

        ArgumentCaptor<GoogleAccountConnection> captor = ArgumentCaptor.forClass(GoogleAccountConnection.class);
        verify(googleAccountConnectionRepository).save(captor.capture());
        assertThat(captor.getValue().isFailed()).isFalse();
        assertThat(captor.getValue().getLastCheckedAt()).isEqualTo(NOW.atZone(ZoneOffset.UTC).toLocalDateTime());
    }

    @Test
    void checkMarksFailedWhenGoogleRefreshFails() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, 1L, "a@b.com", 7L, "scope", "refresh-token", CONNECTED_AT, CONNECTED_AT.plusDays(60),
                CONNECTED_AT, false);
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenThrow(new GoogleOAuthCallException("revoked"));

        service.check(new CheckGoogleConnectionCommand(1L));

        ArgumentCaptor<GoogleAccountConnection> captor = ArgumentCaptor.forClass(GoogleAccountConnection.class);
        verify(googleAccountConnectionRepository).save(captor.capture());
        assertThat(captor.getValue().isFailed()).isTrue();
    }

    @Test
    void checkThrowsWhenNotConnected() {
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(new CheckGoogleConnectionCommand(1L)))
                .isInstanceOf(GoogleAccountNotConnectedException.class);
    }
}
