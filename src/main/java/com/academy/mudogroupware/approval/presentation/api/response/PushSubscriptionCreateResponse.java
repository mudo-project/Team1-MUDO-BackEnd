package com.academy.mudogroupware.approval.presentation.api.response;

public record PushSubscriptionCreateResponse(
        Long subscriptionId
) {

    public static PushSubscriptionCreateResponse from(Long subscriptionId) {
        return new PushSubscriptionCreateResponse(subscriptionId);
    }
}
