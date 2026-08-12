package com.academy.mudogroupware.revenuereport.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadCountResponse(
        @Schema(description = "안읽은 리포트 수", example = "1") long unreadCount
) {
    public static UnreadCountResponse from(long count) {
        return new UnreadCountResponse(count);
    }
}
