package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.auth.application.usecase.TokenRevokerUseCase;
import com.academy.mudogroupware.users.application.usecase.LogoutUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutUseCase {

    private final TokenRevokerUseCase tokenRevokerUseCase;

    @Override
    public void logout(Long userId) {
        tokenRevokerUseCase.revoke(userId);
    }
}
