package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.event.GoogleAccountConnectedEvent;
import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class CompleteGoogleAccountConnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");

    @Mock private GoogleOAuthStatePort googleOAuthStatePort;
    @Mock private GoogleOAuthPort googleOAuthPort;
    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CompleteGoogleAccountConnectionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CompleteGoogleAccountConnectionService(
                googleOAuthStatePort, googleOAuthPort, googleAccountConnectionRepository, clock, eventPublisher);
    }

    @Test
    void completeSavesNewConnectionWhenNoneExisted() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        GoogleOAuthStateClaims claims = new GoogleOAuthStateClaims(7L,false);
        when(googleOAuthStatePort.verify("state")).thenReturn(claims);
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "refresh-token", "scope", 3600L));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("academy@mudo.co.kr");
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        service.complete(command);

        verify(googleOAuthPort, never()).revoke(any());
        ArgumentCaptor<GoogleAccountConnection> captor = ArgumentCaptor.forClass(GoogleAccountConnection.class);
        verify(googleAccountConnectionRepository).save(captor.capture());
        GoogleAccountConnection saved = captor.getValue();
        assertThat(saved.getGoogleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(saved.getConnectedByUserId()).isEqualTo(7L);
        assertThat(saved.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(saved.getRefreshTokenExpiresAt())
                .isEqualTo(NOW.atZone(ZoneOffset.UTC).toLocalDateTime().plusHours(1));
    }

    @Test
    void completePublishesRevocationEventAndReplacesExistingConnection() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L,true));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "new-refresh-token", "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("new@mudo.co.kr");
        GoogleAccountConnection existing = GoogleAccountConnection.restore(
                10L, "old@mudo.co.kr", 5L, "scope", "old-refresh-token",
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().plusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30), false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(existing));

        service.complete(command);

        verify(googleOAuthPort, never()).revoke(any());
        verify(googleAccountConnectionRepository).deleteAll();
        verify(googleAccountConnectionRepository).save(any(GoogleAccountConnection.class));
        ArgumentCaptor<OldGoogleRefreshTokenRevocationRequestedEvent> captor =
                ArgumentCaptor.forClass(OldGoogleRefreshTokenRevocationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldRefreshToken()).isEqualTo("old-refresh-token");
    }

    @Test
    void completeThrowsWhenStateIsInvalid() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "bad-state");
        when(googleOAuthStatePort.verify("bad-state"))
                .thenThrow(new com.academy.mudogroupware.google.domain.exception.GoogleOAuthStateInvalidException());

        assertThatThrownBy(() -> service.complete(command))
                .isInstanceOf(com.academy.mudogroupware.google.domain.exception.GoogleOAuthStateInvalidException.class);
        verify(googleAccountConnectionRepository, never()).save(any());
    }

    @Test
    void completeWrapsOAuthCallFailureAsGoogleOAuthFailedException() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L,false));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenThrow(new GoogleOAuthCallException("failed"));

        assertThatThrownBy(() -> service.complete(command))
                .isInstanceOf(GoogleOAuthFailedException.class);
        verify(googleAccountConnectionRepository, never()).save(any());
    }

    @Test
    void completeThrowsWhenRefreshTokenIsMissing() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L,false));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", null, "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("academy@mudo.co.kr");

        assertThatThrownBy(() -> service.complete(command))
                .isInstanceOf(GoogleOAuthFailedException.class);
        verify(googleAccountConnectionRepository, never()).save(any());
    }

    @Test
    void completePublishesUnchangedEventOnFirstConnection() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L, false));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "refresh-token", "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("academy@mudo.co.kr");
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        service.complete(command);

        ArgumentCaptor<GoogleAccountConnectedEvent> captor = ArgumentCaptor.forClass(GoogleAccountConnectedEvent.class);
        InOrder inOrder = Mockito.inOrder(googleAccountConnectionRepository, eventPublisher);
        inOrder.verify(googleAccountConnectionRepository).save(any(GoogleAccountConnection.class));
        inOrder.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountChanged()).isFalse();
    }

    @Test
    void completeDoesNotPublishEventWhenSaveFails() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L, false));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "refresh-token", "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("academy@mudo.co.kr");
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());
        when(googleAccountConnectionRepository.save(any(GoogleAccountConnection.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> service.complete(command)).isInstanceOf(RuntimeException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void completePublishesUnchangedEventOnSameAccountReconnection() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L, false));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "new-refresh-token", "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("same@mudo.co.kr");
        GoogleAccountConnection existing = GoogleAccountConnection.restore(
                10L, "same@mudo.co.kr", 5L, "scope", "old-refresh-token",
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().plusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30), false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(existing));

        service.complete(command);

        ArgumentCaptor<GoogleAccountConnectedEvent> captor = ArgumentCaptor.forClass(GoogleAccountConnectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountChanged()).isFalse();
    }

    @Test
    void completePublishesChangedEventOnAccountReplacement() {
        CompleteGoogleConnectionCommand command = new CompleteGoogleConnectionCommand("auth-code", "state");
        when(googleOAuthStatePort.verify("state")).thenReturn(new GoogleOAuthStateClaims(7L, true));
        when(googleOAuthPort.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new GoogleTokenExchangeResult("access-token", "new-refresh-token", "scope"));
        when(googleOAuthPort.fetchAccountEmail("access-token")).thenReturn("new@mudo.co.kr");
        GoogleAccountConnection existing = GoogleAccountConnection.restore(
                10L, "old@mudo.co.kr", 5L, "scope", "old-refresh-token",
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().plusDays(30),
                NOW.atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(30), false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(existing));

        service.complete(command);

        ArgumentCaptor<GoogleAccountConnectedEvent> captor = ArgumentCaptor.forClass(GoogleAccountConnectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountChanged()).isTrue();
    }
}
