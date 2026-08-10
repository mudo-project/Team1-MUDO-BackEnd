package com.academy.mudogroupware.google.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.port.GoogleOAuthCallException;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;
import com.academy.mudogroupware.google.application.usecase.StartGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthFailedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StartGoogleAccountConnectionService implements StartGoogleAccountConnectionUseCase {

    private final GoogleOAuthStatePort googleOAuthStatePort;
    private final GoogleOAuthPort googleOAuthPort;

    @Override
    public String start(StartGoogleConnectionCommand command) {
        String state = googleOAuthStatePort.sign(new GoogleOAuthStateClaims(
                command.userId(), command.forceAccountSelection()));
        try {
            return googleOAuthPort.buildAuthorizationUrl(state, command.forceAccountSelection());
        } catch (GoogleOAuthCallException e) {
            throw new GoogleOAuthFailedException(e);
        }
    }
}
