package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenRevokedException;
import com.academy.mudogroupware.google.application.usecase.CheckGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckGoogleAccountConnectionService implements CheckGoogleAccountConnectionUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleOAuthPort googleOAuthPort;
    private final Clock clock;

    @Override
    public void check() {
        GoogleAccountConnection connection = googleAccountConnectionRepository
                .find()
                .orElseThrow(GoogleAccountNotConnectedException::new);

        try {
            googleOAuthPort.refreshAccessToken(connection.getRefreshToken());
            connection.markCheckResult(LocalDateTime.now(clock), true);
        } catch (GoogleTokenRevokedException e) {
            connection.markCheckResult(LocalDateTime.now(clock), false);
        } catch (GoogleOAuthCallException e) {
            connection.markCheckAttempted(LocalDateTime.now(clock));
        }

        googleAccountConnectionRepository.save(connection);
    }
}
