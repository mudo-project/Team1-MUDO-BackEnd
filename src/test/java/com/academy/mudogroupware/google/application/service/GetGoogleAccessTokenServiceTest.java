package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.application.port.GoogleTokenRevokedException;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountConnectionInvalidException;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;
import com.academy.mudogroupware.google.infrastructure.external.google.GoogleOAuthProperties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

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
        verify(googleAccountConnectionRepository, never()).save(any());
    }

    @Test
    void getAccessTokenReturnsCachedTokenWithoutCallingGoogleAgainWithinExpiry() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "new-access-token", "refresh-token", "openid email drive.file", null, 3600L));

        String first = service.getAccessToken();
        String second = service.getAccessToken();

        assertThat(first).isEqualTo("new-access-token");
        assertThat(second).isEqualTo("new-access-token");
        verify(googleOAuthPort, org.mockito.Mockito.times(1)).refreshAccessToken("refresh-token");
    }

    @Test
    void getAccessTokenCallsGoogleAgainAfterCacheExpiresWithBuffer() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "new-access-token", "refresh-token", "openid email drive.file", null, 3600L));

        MutableClock mutableClock = new MutableClock(NOW, ZoneOffset.UTC);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id", "client-secret", "https://example.com/callback", "openid email drive.file", "/");
        GetGoogleAccessTokenService serviceWithMutableClock = new GetGoogleAccessTokenService(
                googleAccountConnectionRepository, googleOAuthPort, mutableClock, properties);

        serviceWithMutableClock.getAccessToken();
        // 캐시 만료 시각(발급 후 3600초 - 60초 버퍼)을 막 지난 시점으로 이동
        mutableClock.advance(Duration.ofSeconds(3600 - 60 + 1));
        serviceWithMutableClock.getAccessToken();

        verify(googleOAuthPort, org.mockito.Mockito.times(2)).refreshAccessToken("refresh-token");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    void getAccessTokenCallsGoogleEveryTimeWhenExpirationUnknown() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult("new-access-token", "refresh-token", "openid email drive.file"));

        service.getAccessToken();
        service.getAccessToken();

        verify(googleOAuthPort, org.mockito.Mockito.times(2)).refreshAccessToken("refresh-token");
    }

    @Test
    void getAccessTokenDoesNotReuseCachedTokenAfterAccountSwitch() {
        GoogleAccountConnection oldConnection = GoogleAccountConnection.restore(
                10L, "old@mudo.co.kr", 7L, "openid email drive.file", "old-refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        GoogleAccountConnection newConnection = GoogleAccountConnection.restore(
                11L, "new@mudo.co.kr", 7L, "openid email drive.file", "new-refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find())
                .thenReturn(Optional.of(oldConnection))
                .thenReturn(Optional.of(newConnection));
        when(googleOAuthPort.refreshAccessToken("old-refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "old-access-token", "old-refresh-token", "openid email drive.file", null, 3600L));
        when(googleOAuthPort.refreshAccessToken("new-refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "new-access-token", "new-refresh-token", "openid email drive.file", null, 3600L));

        String beforeSwitch = service.getAccessToken();
        String afterSwitch = service.getAccessToken();

        assertThat(beforeSwitch).isEqualTo("old-access-token");
        assertThat(afterSwitch).isEqualTo("new-access-token");
        verify(googleOAuthPort).refreshAccessToken("old-refresh-token");
        verify(googleOAuthPort).refreshAccessToken("new-refresh-token");
    }

    @Test
    void getAccessTokenCallsGoogleOnlyOnceForConcurrentRequestsOnCacheMiss() throws Exception {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token")).thenAnswer(invocation -> {
            Thread.sleep(100);
            return new GoogleTokenExchangeResult(
                    "new-access-token", "refresh-token", "openid email drive.file", null, 3600L);
        });

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.getAccessToken();
            }));
        }
        ready.await();
        start.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();

        assertThat(results).allMatch("new-access-token"::equals);
        verify(googleOAuthPort, org.mockito.Mockito.times(1)).refreshAccessToken("refresh-token");
    }

    @Test
    void getAccessTokenMarksFailedAndThrowsInvalidWhenTokenRevoked() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenThrow(new GoogleTokenRevokedException("revoked", null));

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(GoogleAccountConnectionInvalidException.class);

        ArgumentCaptor<GoogleAccountConnection> captor = ArgumentCaptor.forClass(GoogleAccountConnection.class);
        verify(googleAccountConnectionRepository).save(captor.capture());
        assertThat(captor.getValue().isFailed()).isTrue();
    }

    @Test
    void getAccessTokenLogsDebugOnCacheHit() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "new-access-token", "refresh-token", "openid email drive.file", null, 3600L));

        List<ILoggingEvent> events = captureLogs(Level.DEBUG, () -> {
            service.getAccessToken();
            service.getAccessToken();
        });

        assertThat(events)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                    assertThat(event.getFormattedMessage()).contains("event=google_access_token_cache_hit");
                });
    }

    @Test
    void getAccessTokenLogsInfoOnSuccessfulRefresh() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleTokenExchangeResult(
                        "new-access-token", "refresh-token", "openid email drive.file", null, 3600L));

        List<ILoggingEvent> events = captureLogs(Level.INFO, () -> service.getAccessToken());

        assertThat(events)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.INFO);
                    assertThat(event.getFormattedMessage()).contains("event=google_access_token_refreshed");
                });
    }

    @Test
    void getAccessTokenLogsWarnWhenTokenRevokedDuringRefresh() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenThrow(new GoogleTokenRevokedException("revoked", null));

        List<ILoggingEvent> events = captureLogs(Level.WARN, () -> {
            try {
                service.getAccessToken();
            } catch (GoogleAccountConnectionInvalidException ignored) {
                // 로그만 확인하면 되므로 예외는 무시
            }
        });

        assertThat(events)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("event=google_access_token_refresh_failed");
                });
    }

    @Test
    void getAccessTokenLogsWarnWhenGoogleCallFails() {
        GoogleAccountConnection connection = GoogleAccountConnection.restore(
                10L, "academy@mudo.co.kr", 7L, "openid email drive.file", "refresh-token",
                CONNECTED_AT, CONNECTED_AT.plusDays(60), CONNECTED_AT, false);
        when(googleAccountConnectionRepository.find()).thenReturn(Optional.of(connection));
        when(googleOAuthPort.refreshAccessToken("refresh-token"))
                .thenThrow(new GoogleOAuthCallException("구글 토큰 발급에 실패했습니다."));

        List<ILoggingEvent> events = captureLogs(Level.WARN, () -> {
            try {
                service.getAccessToken();
            } catch (GoogleOAuthFailedException ignored) {
                // 로그만 확인하면 되므로 예외는 무시
            }
        });

        assertThat(events)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("event=google_access_token_refresh_failed");
                });
    }

    private List<ILoggingEvent> captureLogs(Level level, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(GetGoogleAccessTokenService.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(level);
        logger.addAppender(appender);
        try {
            action.run();
            return appender.list;
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }
    }
}
