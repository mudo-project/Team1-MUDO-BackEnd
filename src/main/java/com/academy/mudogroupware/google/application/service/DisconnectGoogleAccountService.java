package com.academy.mudogroupware.google.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.usecase.DisconnectGoogleAccountUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DisconnectGoogleAccountService implements DisconnectGoogleAccountUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleOAuthPort googleOAuthPort;

    @Override
    public void disconnect() {
        GoogleAccountConnection connection = googleAccountConnectionRepository
                .find()
                .orElseThrow(GoogleAccountNotConnectedException::new);

        googleOAuthPort.revoke(connection.getRefreshToken());
        googleAccountConnectionRepository.deleteAll();
    }
}
