package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

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

import lombok.RequiredArgsConstructor;

// noRollbackFor: 영구 오류 감지 시 markCheckResult(false)를 save()한 뒤
// GoogleAccountConnectionInvalidException을 던지는데, 기본 롤백 규칙(RuntimeException 전체
// 롤백)이면 그 save()까지 함께 롤백돼 failed=true가 유실된다. 이 예외로는 롤백하지 않아야
// 자기 치유 감지 결과가 실제로 저장된다.
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = GoogleAccountConnectionInvalidException.class)
public class GetGoogleAccessTokenService implements GetGoogleAccessTokenUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleOAuthPort googleOAuthPort;
    private final Clock clock;
    private final RequiredGoogleScopePort requiredGoogleScopePort;

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
            GoogleTokenExchangeResult result = googleOAuthPort.refreshAccessToken(connection.getRefreshToken());
            return result.accessToken();
        } catch (GoogleTokenRevokedException e) {
            connection.markCheckResult(LocalDateTime.now(clock), false);
            googleAccountConnectionRepository.save(connection);
            throw new GoogleAccountConnectionInvalidException();
        } catch (GoogleOAuthCallException e) {
            throw new GoogleOAuthFailedException(e);
        }
    }
}
