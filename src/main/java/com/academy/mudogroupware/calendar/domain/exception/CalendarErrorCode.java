package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CalendarErrorCode implements ErrorCode {

    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "CALENDAR_400_1", "일정 제목은 비어 있을 수 없습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "CALENDAR_400_2", "종료 시각은 시작 시각보다 이전일 수 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR_404_1", "일정을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
