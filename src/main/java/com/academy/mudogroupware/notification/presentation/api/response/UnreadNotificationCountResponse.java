package com.academy.mudogroupware.notification.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadNotificationCountResponse(
        @Schema(description = "안읽은 알림 개수", example = "3") long unreadCount) {
}
