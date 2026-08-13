package com.academy.mudogroupware.notification.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationResponseCode implements ResponseCode {

    NOTIFICATION_LIST_RETRIEVED("NOTIFICATION_200_1", "알림 목록 조회에 성공했습니다."),
    UNREAD_COUNT_RETRIEVED("NOTIFICATION_200_2", "안읽은 알림 개수 조회에 성공했습니다."),
    NOTIFICATION_READ("NOTIFICATION_200_3", "알림 읽음 처리에 성공했습니다."),
    NOTIFICATION_DELETED("NOTIFICATION_200_4", "알림 삭제에 성공했습니다."),
    READ_NOTIFICATIONS_DELETED("NOTIFICATION_200_5", "읽은 알림 일괄 삭제에 성공했습니다.");

    private final String code;
    private final String message;
}
