package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.application.port.GoogleTokenRevokedException;
import com.academy.mudogroupware.google.application.port.RequiredGoogleScopePort;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountConnectionInvalidException;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// noRollbackFor: 영구 오류 감지 시 markCheckResult(false)를 save()한 뒤
// GoogleAccountConnectionInvalidException을 던지는데, 기본 롤백 규칙(RuntimeException 전체
// 롤백)이면 그 save()까지 함께 롤백돼 failed=true가 유실된다. 이 예외로는 롤백하지 않아야
// 자기 치유 감지 결과가 실제로 저장된다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = GoogleAccountConnectionInvalidException.class)
public class GetGoogleAccessTokenService implements GetGoogleAccessTokenUseCase {

    // Google이 매번 내려주는 access token 만료 시각보다 이만큼 일찍 캐시를 만료시켜서,
    // 캐시된 토큰을 반환한 직후 실제 Drive API 호출 도중 만료되는 상황을 피한다.
    private static final long ACCESS_TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleOAuthPort googleOAuthPort;
    private final Clock clock;
    private final RequiredGoogleScopePort requiredGoogleScopePort;

    // 키를 refreshToken으로 둬서, 계정 교체로 refreshToken이 바뀌면 옛 캐시 항목은 더 이상
    // 조회되지 않고(자연 무효화) maximumSize에 의해 밀려난다 — 별도 무효화 이벤트 구독이 필요 없다.
    private final Cache<String, CachedAccessToken> accessTokenCache = Caffeine.newBuilder().maximumSize(4).build();

    private record CachedAccessToken(String accessToken, Instant expiresAt) {
    }

    @Override
    public String getAccessToken() {
        GoogleAccountConnection connection = googleAccountConnectionRepository.find()
                .orElseThrow(GoogleAccountNotConnectedException::new);

        GoogleConnectionStatus status = connection.deriveStatus(LocalDateTime.now(clock));
        if (status == GoogleConnectionStatus.FAILED || status == GoogleConnectionStatus.EXPIRED
                || !connection.hasAllScopes(requiredGoogleScopePort.requiredScopes())) {
            throw new GoogleAccountConnectionInvalidException();
        }

        try {
            return getOrRefreshAccessToken(connection);
        } catch (GoogleTokenRevokedException e) {
            log.warn("event=google_access_token_refresh_failed connectionId={} reason=token_revoked",
                    connection.getId());
            connection.markCheckResult(LocalDateTime.now(clock), false);
            googleAccountConnectionRepository.save(connection);
            throw new GoogleAccountConnectionInvalidException();
        } catch (GoogleOAuthCallException e) {
            log.warn("event=google_access_token_refresh_failed connectionId={} reason=google_call_failed message={}",
                    connection.getId(), e.getMessage());
            throw new GoogleOAuthFailedException(e);
        }
    }

    // ConcurrentMap#compute()는 같은 키에 대해 원자적으로 실행되므로, 같은 refreshToken으로
    // 동시에 들어온 요청들은 이 블록 안에서 직렬화된다 — 캐시 미스 시 Google 호출이 단 1회만
    // 나가는 single-flight를 별도 락 없이 보장한다.
    private String getOrRefreshAccessToken(GoogleAccountConnection connection) {
        String refreshToken = connection.getRefreshToken();
        ConcurrentMap<String, CachedAccessToken> cacheMap = accessTokenCache.asMap();
        AtomicReference<String> accessTokenHolder = new AtomicReference<>();

        cacheMap.compute(refreshToken, (key, existing) -> {
            Instant now = clock.instant();
            if (existing != null && now.isBefore(existing.expiresAt())) {
                log.debug("event=google_access_token_cache_hit connectionId={}", connection.getId());
                accessTokenHolder.set(existing.accessToken());
                return existing;
            }

            GoogleTokenExchangeResult result = googleOAuthPort.refreshAccessToken(refreshToken);
            log.info("event=google_access_token_refreshed connectionId={}", connection.getId());
            accessTokenHolder.set(result.accessToken());
            return toCacheEntryIfExpirationKnown(result, now);
        });

        return accessTokenHolder.get();
    }

    private CachedAccessToken toCacheEntryIfExpirationKnown(GoogleTokenExchangeResult result, Instant now) {
        if (result.accessTokenExpiresInSeconds() == null) {
            return null;
        }
        Instant expiresAt = now.plusSeconds(result.accessTokenExpiresInSeconds())
                .minusSeconds(ACCESS_TOKEN_EXPIRY_BUFFER_SECONDS);
        return expiresAt.isAfter(now) ? new CachedAccessToken(result.accessToken(), expiresAt) : null;
    }
}
