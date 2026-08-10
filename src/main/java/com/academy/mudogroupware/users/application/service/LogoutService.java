package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.auth.application.usecase.TokenRevokerUseCase;
import com.academy.mudogroupware.users.application.usecase.LogoutUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutUseCase {

    private final TokenRevokerUseCase tokenRevokerUseCase;

    @Override
    public void logout(Long userId) {
        log.info("event=auth_logout_시작 userId={}", userId);
        tokenRevokerUseCase.revoke(userId);
        log.info("event=auth_logout_완료 userId={}", userId);
    }
}
