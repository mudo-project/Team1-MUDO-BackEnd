package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class GetGoogleAccountConnectionStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;

    private GetGoogleAccountConnectionStatusService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new GetGoogleAccountConnectionStatusService(googleAccountConnectionRepository, clock);
    }

    @Test
    void getStatusReturnsNotConnectedWhenConnectionDoesNotExist() {
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        assertThat(service.getStatus()).isEqualTo(GoogleConnectionStatus.NOT_CONNECTED);
    }

    @Test
    void getStatusDerivesConnectionStatusFromConnectionOnly() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "scope", "refresh-token", CONNECTED_AT,
                LocalDateTime.of(2026, 8, 7, 0, 0), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        assertThat(service.getStatus()).isEqualTo(GoogleConnectionStatus.EXPIRING);
    }
}
