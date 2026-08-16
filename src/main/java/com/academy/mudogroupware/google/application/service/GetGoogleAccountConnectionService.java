package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.query.GoogleAccountConnectionView;
import com.academy.mudogroupware.google.application.port.GoogleConnectionUserDirectoryPort;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGoogleAccountConnectionService implements GetGoogleAccountConnectionUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final GoogleConnectionUserDirectoryPort googleConnectionUserDirectoryPort;
    private final Clock clock;

    @Override
    public Optional<GoogleAccountConnectionView> getConnection() {
        return googleAccountConnectionRepository.find().map(this::toView);
    }

    private GoogleAccountConnectionView toView(GoogleAccountConnection connection) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new GoogleAccountConnectionView(
                connection.getGoogleEmail(),
                connection.getConnectedByUserId(),
                googleConnectionUserDirectoryPort.findByUserId(connection.getConnectedByUserId())
                        .map(user -> user.userName())
                        .orElse(null),
                connection.getScope(),
                connection.getConnectedAt(),
                connection.getRefreshTokenExpiresAt(),
                connection.getLastCheckedAt(),
                connection.deriveStatus(now));
    }
}
