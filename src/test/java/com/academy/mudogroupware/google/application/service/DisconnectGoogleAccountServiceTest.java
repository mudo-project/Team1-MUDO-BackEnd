package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class DisconnectGoogleAccountServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DisconnectGoogleAccountService service;

    @BeforeEach
    void setUp() {
        service = new DisconnectGoogleAccountService(googleAccountConnectionRepository, eventPublisher);
    }

    @Test
    void disconnectDeletesConnectionAndPublishesRevocationEvent() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "a@b.com", 7L, "scope", "refresh-token", NOW.minusDays(1), NOW.plusDays(59),
                NOW.minusDays(1), false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        service.disconnect();

        verify(googleAccountConnectionRepository).deleteAll();
        ArgumentCaptor<OldGoogleRefreshTokenRevocationRequestedEvent> captor =
                ArgumentCaptor.forClass(OldGoogleRefreshTokenRevocationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void disconnectThrowsWhenNotConnected() {
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(service::disconnect)
                .isInstanceOf(GoogleAccountNotConnectedException.class);
        verify(googleAccountConnectionRepository, never()).deleteAll();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
