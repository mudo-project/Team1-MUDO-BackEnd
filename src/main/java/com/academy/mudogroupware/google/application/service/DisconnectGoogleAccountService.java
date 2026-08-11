package com.academy.mudogroupware.google.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    // 연동을 DB에서 삭제하고, 저장된 리프레시 토큰의 구글 측 폐기는 커밋 이후로 미룬다.
    @Override
    public void disconnect() {
        GoogleAccountConnection connection = googleAccountConnectionRepository
                .find()
                .orElseThrow(GoogleAccountNotConnectedException::new);

        googleAccountConnectionRepository.deleteAll();
        eventPublisher.publishEvent(new OldGoogleRefreshTokenRevocationRequestedEvent(connection.getRefreshToken()));
    }
}
