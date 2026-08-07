package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;
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

        Optional<GoogleAccountConnection> existing =
                googleAccountConnectionRepository.findByAcademyId(claims.academyId());
        existing.ifPresent(connection -> googleOAuthPort.revoke(connection.getRefreshToken()));
        googleAccountConnectionRepository.deleteByAcademyId(claims.academyId());

        GoogleAccountConnection connection = GoogleAccountConnection.connect(
                claims.academyId(), googleEmail, claims.userId(), tokens.scope(), tokens.refreshToken(),
                LocalDateTime.now(clock));
        googleAccountConnectionRepository.save(connection);
    }
}
