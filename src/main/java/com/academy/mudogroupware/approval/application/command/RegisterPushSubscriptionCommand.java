package com.academy.mudogroupware.approval.application.command;

public record RegisterPushSubscriptionCommand(Long userId, String endpoint, String p256dh, String auth) {
}
