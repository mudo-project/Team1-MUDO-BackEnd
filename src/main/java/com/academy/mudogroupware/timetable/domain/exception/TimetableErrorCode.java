package com.academy.mudogroupware.timetable.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimetableErrorCode implements ErrorCode {

    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "TIMETABLE_400_1", "시간표 세트 이름은 비어 있을 수 없습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "TIMETABLE_400_2", "종료일은 시작일보다 이전일 수 없습니다."),
    DUPLICATE_CLASSROOM_CODE(HttpStatus.BAD_REQUEST, "TIMETABLE_400_3", "강의실 코드는 세트 내에서 중복될 수 없습니다."),
    SET_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMETABLE_404_1", "시간표 세트를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
