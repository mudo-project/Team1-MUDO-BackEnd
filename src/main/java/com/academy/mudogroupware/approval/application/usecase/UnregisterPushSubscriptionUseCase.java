package com.academy.mudogroupware.approval.application.usecase;

public interface UnregisterPushSubscriptionUseCase {

    void unregister(Long userId, String endpoint);
}
