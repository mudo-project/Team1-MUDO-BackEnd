package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleTokenExchangeResult;
import com.academy.mudogroupware.google.application.port.RequiredGoogleScopePort;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountConnectionInvalidException;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGoogleAccessTokenService implements GetGoogleAccessTokenUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleOAuthPort googleOAuthPort;
    private final Clock clock;
    private final RequiredGoogleScopePort requiredGoogleScopePort;

    @Override
    public String getAccessToken(Long academyId) {
        GoogleAccountConnection connection = googleAccountConnectionRepository.findByAcademyId(academyId)
                .orElseThrow(GoogleAccountNotConnectedException::new);

        GoogleConnectionStatus status = connection.deriveStatus(
                LocalDateTime.now(clock), requiredGoogleScopePort.requiredScopes());
        if (status == GoogleConnectionStatus.FAILED || status == GoogleConnectionStatus.EXPIRED) {
            throw new GoogleAccountConnectionInvalidException();
        }

        try {
            GoogleTokenExchangeResult result = googleOAuthPort.refreshAccessToken(connection.getRefreshToken());
            return result.accessToken();
        } catch (GoogleOAuthCallException e) {
            throw new GoogleOAuthFailedException(e);
        }
    }
}
