package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.RegisterPushSubscriptionCommand;
import com.academy.mudogroupware.approval.application.usecase.RegisterPushSubscriptionUseCase;
import com.academy.mudogroupware.approval.domain.model.PushSubscription;
import com.academy.mudogroupware.approval.domain.repository.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterPushSubscriptionService implements RegisterPushSubscriptionUseCase {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final Clock clock;

    @Override
    public Long register(RegisterPushSubscriptionCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        PushSubscription pushSubscription = PushSubscription.create(
                command.userId(), command.endpoint(), command.p256dh(), command.auth(), now);

        return pushSubscriptionRepository.save(pushSubscription).getId();
    }
}
