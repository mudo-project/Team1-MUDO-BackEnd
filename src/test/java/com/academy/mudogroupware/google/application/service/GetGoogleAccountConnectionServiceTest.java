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
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;
import com.academy.mudogroupware.google.infrastructure.external.google.GoogleOAuthProperties;

@ExtendWith(MockitoExtension.class)
class GetGoogleAccountConnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T00:00:00Z");
    private static final LocalDateTime CONNECTED_AT = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock private GoogleAccountConnectionRepository googleAccountConnectionRepository;

    private GetGoogleAccountConnectionService service;

    private GetGoogleAccountConnectionService serviceWithScope(String requiredScope) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", requiredScope, "/");
        return new GetGoogleAccountConnectionService(googleAccountConnectionRepository, clock, properties);
    }

    @BeforeEach
    void setUp() {
        service = serviceWithScope("scope");
    }

    @Test
    void getConnectionReturnsEmptyWhenNotConnected() {
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.empty());

        assertThat(service.getConnection(1L)).isEmpty();
    }

    @Test
    void getConnectionReturnsViewWithDerivedStatus() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, 1L, "academy@mudo.co.kr", 7L, "scope", "refresh-token", CONNECTED_AT,
                CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.of(connection));

        Optional<GoogleAccountConnectionView> view = service.getConnection(1L);

        assertThat(view).isPresent();
        assertThat(view.get().googleEmail()).isEqualTo("academy@mudo.co.kr");
        assertThat(view.get().connectedByUserId()).isEqualTo(7L);
        assertThat(view.get().status()).isEqualTo(GoogleConnectionStatus.CONNECTED);
    }

    @Test
    void getConnectionReturnsFailedWhenStoredScopeMissingNewlyRequiredScope() {
        GetGoogleAccountConnectionService serviceWithExpandedScope = serviceWithScope("openid email drive.file");
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, 1L, "academy@mudo.co.kr", 7L, "openid email", "refresh-token", CONNECTED_AT,
                CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.findByAcademyId(1L)).thenReturn(Optional.of(connection));

        Optional<GoogleAccountConnectionView> view = serviceWithExpandedScope.getConnection(1L);

        assertThat(view).isPresent();
        assertThat(view.get().status()).isEqualTo(GoogleConnectionStatus.FAILED);
    }
}
