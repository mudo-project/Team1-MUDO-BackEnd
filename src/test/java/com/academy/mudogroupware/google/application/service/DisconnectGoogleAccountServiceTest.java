package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class DisconnectGoogleAccountServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private GoogleOAuthPort googleOAuthPort;

    private DisconnectGoogleAccountService service;

    @BeforeEach
    void setUp() {
        service = new DisconnectGoogleAccountService(googleAccountConnectionRepository, googleOAuthPort);
    }

    @Test
    void disconnectRevokesTokenAndDeletesConnection() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "a@b.com", 7L, "scope", "refresh-token", NOW.minusDays(1), NOW.plusDays(59),
                NOW.minusDays(1), false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        service.disconnect();

        InOrder order = inOrder(googleOAuthPort, googleAccountConnectionRepository);
        order.verify(googleOAuthPort).revoke("refresh-token");
        order.verify(googleAccountConnectionRepository).deleteAll();
    }

    @Test
    void disconnectThrowsWhenNotConnected() {
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        assertThatThrownBy(service::disconnect)
                .isInstanceOf(GoogleAccountNotConnectedException.class);
        verify(googleAccountConnectionRepository, never()).deleteAll();
    }
}
