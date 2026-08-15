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

import com.academy.mudogroupware.google.application.query.GoogleAccountConnectionView;
import com.academy.mudogroupware.google.application.port.GoogleConnectionUserDirectoryPort;
import com.academy.mudogroupware.google.application.port.GoogleConnectionUserInfo;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

@ExtendWith(MockitoExtension.class)
class GetGoogleAccountConnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;
    @Mock private GoogleConnectionUserDirectoryPort googleConnectionUserDirectoryPort;

    private GetGoogleAccountConnectionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new GetGoogleAccountConnectionService(
                googleAccountConnectionRepository, googleConnectionUserDirectoryPort, clock);
    }

    @Test
    void getConnectionReturnsEmptyWhenNotConnected() {
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.empty());

        assertThat(service.getConnection()).isEmpty();
    }

    @Test
    void getConnectionReturnsViewWithDerivedStatus() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "scope", "refresh-token", CONNECTED_AT,
                null, CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleConnectionUserDirectoryPort.findByUserId(7L))
                .thenReturn(Optional.of(new GoogleConnectionUserInfo(7L, "원장")));

        Optional<GoogleAccountConnectionView> view = service.getConnection();

        assertThat(view).isPresent();
        assertThat(view.get().googleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(view.get().connectedByUserId()).isEqualTo(7L);
        assertThat(view.get().connectedByUserName()).isEqualTo("원장");
        assertThat(view.get().status()).isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void getConnectionReturnsNullConnectorNameWhenHistoricalUserDoesNotExist() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "scope", "refresh-token", CONNECTED_AT,
                null, CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleConnectionUserDirectoryPort.findByUserId(7L)).thenReturn(Optional.empty());

        Optional<GoogleAccountConnectionView> view = service.getConnection();

        assertThat(view).isPresent();
        assertThat(view.get().connectedByUserName()).isNull();
    }

    @Test
    void getConnectionKeepsConnectedWhenStoredScopeMissesNewlyRequiredScope() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email", "refresh-token", CONNECTED_AT,
                null, CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));

        Optional<GoogleAccountConnectionView> view = service.getConnection();

        assertThat(view).isPresent();
        assertThat(view.get().status()).isEqualTo(GoogleConnectionStatus.CONNECTED);
    }
}
