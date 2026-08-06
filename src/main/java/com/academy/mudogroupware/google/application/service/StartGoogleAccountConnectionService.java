package com.academy.mudogroupware.google.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;
import com.academy.mudogroupware.google.application.usecase.StartGoogleAccountConnectionUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StartGoogleAccountConnectionService implements StartGoogleAccountConnectionUseCase {

    private final GoogleOAuthStatePort googleOAuthStatePort;
    private final GoogleOAuthPort googleOAuthPort;

    @Override
    public String start(StartGoogleConnectionCommand command) {
        String state = googleOAuthStatePort.sign(new GoogleOAuthStateClaims(
                command.academyId(), command.userId(), command.forceAccountSelection()));
        return googleOAuthPort.buildAuthorizationUrl(state, command.forceAccountSelection());
    }
}
