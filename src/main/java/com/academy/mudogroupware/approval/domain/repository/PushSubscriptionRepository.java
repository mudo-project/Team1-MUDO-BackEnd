package com.academy.mudogroupware.approval.domain.repository;

import java.util.List;

import com.academy.mudogroupware.approval.domain.model.PushSubscription;

public interface PushSubscriptionRepository {

    PushSubscription save(PushSubscription pushSubscription);

    List<PushSubscription> findAllByUserId(Long userId);

    void deleteByUserIdAndEndpoint(Long userId, String endpoint);
}
