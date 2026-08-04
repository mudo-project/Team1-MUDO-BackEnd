package com.academy.mudogroupware.approval.presentation.api.request;

import com.academy.mudogroupware.approval.application.command.RegisterPushSubscriptionCommand;

import jakarta.validation.constraints.NotBlank;

public record RegisterPushSubscriptionRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth
) {

    public RegisterPushSubscriptionCommand toCommand(Long userId) {
        return new RegisterPushSubscriptionCommand(userId, endpoint, p256dh, auth);
    }
}
