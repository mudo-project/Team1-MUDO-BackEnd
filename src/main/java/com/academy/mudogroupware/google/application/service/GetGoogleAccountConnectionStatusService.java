package com.academy.mudogroupware.google.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionStatusUseCase;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGoogleAccountConnectionStatusService implements GetGoogleAccountConnectionStatusUseCase {

    private final GoogleAccountConnectionRepository googleAccountConnectionRepository;
    private final Clock clock;

    @Override
    public GoogleConnectionStatus getStatus() {
        return googleAccountConnectionRepository.find()
                .map(connection -> connection.deriveStatus(LocalDateTime.now(clock)))
                .orElse(GoogleConnectionStatus.NOT_CONNECTED);
    }
}
