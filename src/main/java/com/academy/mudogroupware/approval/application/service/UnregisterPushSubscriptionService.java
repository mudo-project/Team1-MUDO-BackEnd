package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.usecase.UnregisterPushSubscriptionUseCase;
import com.academy.mudogroupware.approval.domain.repository.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UnregisterPushSubscriptionService implements UnregisterPushSubscriptionUseCase {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Override
    public void unregister(Long userId, String endpoint) {
        pushSubscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
    }
}
