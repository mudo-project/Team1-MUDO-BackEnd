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
    INVALID_QUERY_RANGE(HttpStatus.BAD_REQUEST, "CALENDAR_400_3", "date 또는 yearMonth 중 정확히 하나를 지정해야 합니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR_404_1", "일정을 찾을 수 없습니다."),
    EVENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "CALENDAR_409_1", "다른 요청이 먼저 정보를 수정했습니다. 다시 조회한 뒤 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
