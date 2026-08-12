package com.academy.mudogroupware.notification.presentation.api.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import com.academy.mudogroupware.notification.domain.model.Notification;

@Builder
public record NotificationItemResponse(
        @Schema(description = "알림 번호", example = "1") Long notificationId,
        @Schema(description = "알림 타입", example = "APPROVAL_LINE_ACTIVATED") String type,
        @Schema(description = "대상 ID(업무·문서 등)", example = "100") Long targetId,
        @Schema(description = "알림 문구", example = "결재 문서 [휴가 신청서] 결재 차례가 되었습니다") String message,
        @Schema(description = "읽음 여부", example = "false") boolean read,
        @Schema(description = "생성일시", example = "2026-08-13T09:00:00") LocalDateTime createdAt) {

    public static NotificationItemResponse from(Notification notification) {
        return NotificationItemResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .targetId(notification.getTargetId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
