package com.academy.mudogroupware.notification.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    INVALID_DELETE_STATUS_FILTER(HttpStatus.BAD_REQUEST, "NOTIFICATION_400_1",
            "일괄 삭제는 status=READ만 지원합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404_1", "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
