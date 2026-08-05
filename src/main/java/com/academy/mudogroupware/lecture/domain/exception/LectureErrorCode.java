package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LectureErrorCode implements ErrorCode {

    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "LECTURE_400_1", "강의 이름은 비어 있을 수 없습니다."),
    SCHEDULE_REQUIRED(HttpStatus.BAD_REQUEST, "LECTURE_400_2", "요일·시간대는 최소 1개 이상 지정해야 합니다."),
    INVALID_SCHEDULE_TIME(HttpStatus.BAD_REQUEST, "LECTURE_400_3", "시작 시간은 종료 시간보다 빨라야 합니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "LECTURE_403_1", "다른 학원의 강의에는 접근할 수 없습니다."),

    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_404_1", "강의를 찾을 수 없습니다."),
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_404_2", "학생을 찾을 수 없습니다."),

    CLASSROOM_TIME_CONFLICT(HttpStatus.CONFLICT, "LECTURE_409_1", "같은 교실·요일·시간대에 이미 다른 강의가 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
