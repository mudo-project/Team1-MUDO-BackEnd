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
    SET_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMETABLE_404_1", "시간표 세트를 찾을 수 없습니다."),
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMETABLE_404_2", "수업 슬롯을 찾을 수 없습니다."),
    CLASSROOM_TIME_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_409_1", "같은 강의실에 겹치는 시간대의 수업이 이미 있습니다."),
    UNSUPPORTED_SCOPE(HttpStatus.BAD_REQUEST, "TIMETABLE_400_4", "이 적용 범위는 아직 지원하지 않습니다. scope=ALL만 가능합니다."),
    INVALID_EXPORT_COLOR(HttpStatus.BAD_REQUEST, "TIMETABLE_400_5", "내보내기 색상 값은 6자리 16진수(RRGGBB)여야 합니다."),
    EXPORT_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "TIMETABLE_400_6", "내보내기 이미지 크기가 너무 큽니다. 수업 슬롯 수를 줄여주세요."),
    INVALID_SET_CONFIGURATION(HttpStatus.BAD_REQUEST, "TIMETABLE_400_7", "시간표 세트 설정이 올바르지 않습니다."),
    INVALID_SLOT_CONFIGURATION(HttpStatus.BAD_REQUEST, "TIMETABLE_400_8", "수업 슬롯 설정이 올바르지 않습니다."),
    INVALID_CLASSROOM(HttpStatus.BAD_REQUEST, "TIMETABLE_400_9", "강의실 정보가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
