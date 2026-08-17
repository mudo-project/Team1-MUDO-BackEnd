package com.academy.mudogroupware.notification.application.command;

public record CreateNotificationCommand(
        Long recipientUserId, String type, Long targetId, String message, String idempotencyKey) {
}
