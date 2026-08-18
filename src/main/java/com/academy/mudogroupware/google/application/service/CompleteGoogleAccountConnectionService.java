package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.event.GoogleAccountConnectedEvent;
import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.application.usecase.CompleteGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteGoogleAccountConnectionService implements CompleteGoogleAccountConnectionUseCase {

    private final GoogleOAuthStatePort googleOAuthStatePort;
    private final GoogleOAuthPort googleOAuthPort;
    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    // 인가 코드를 토큰으로 교환해 구글 계정 연동을 저장하고, 기존 토큰 폐기·연동 성공 통지를 커밋 이후로 미룬다.
    @Override
    public void complete(CompleteGoogleConnectionCommand command) {
        GoogleOAuthStateClaims claims = googleOAuthStatePort.verify(command.state());

        GoogleTokenExchangeResult tokens;
        String googleEmail;
        try {
            tokens = googleOAuthPort.exchangeAuthorizationCode(command.authorizationCode());
            googleEmail = googleOAuthPort.fetchAccountEmail(tokens.accessToken());
        } catch (GoogleOAuthCallException e) {
            throw new GoogleOAuthFailedException(e);
        }

        if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
            throw new GoogleOAuthFailedException(
                    new IllegalStateException("구글이 리프레시 토큰을 반환하지 않았습니다."));
        }

        Optional<GoogleAccountConnection> existing = googleAccountConnectionRepository.find();
        Optional<String> previousRefreshToken = existing.map(GoogleAccountConnection::getRefreshToken);
        googleAccountConnectionRepository.deleteAll();

        LocalDateTime connectedAt = LocalDateTime.now(clock);
        LocalDateTime refreshTokenExpiresAt = tokens.refreshTokenExpiresInSeconds() == null
                ? null
                : connectedAt.plusSeconds(tokens.refreshTokenExpiresInSeconds());
        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                googleEmail, claims.userId(), tokens.scope(), tokens.refreshToken(),
                connectedAt, refreshTokenExpiresAt);
        googleAccountConnectionRepository.save(connection);

        previousRefreshToken.ifPresent(
                token -> eventPublisher.publishEvent(new OldGoogleRefreshTokenRevocationRequestedEvent(token)));
        eventPublisher.publishEvent(new GoogleAccountConnectedEvent(googleEmail));
    }
}
