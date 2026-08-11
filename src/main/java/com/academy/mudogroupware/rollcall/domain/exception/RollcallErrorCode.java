package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RollcallErrorCode implements ErrorCode {

    ETC_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "ROLLCALL_400_1", "기타 사유를 입력해야 합니다."),
    NO_STUDENTS_SELECTED(HttpStatus.BAD_REQUEST, "ROLLCALL_400_2", "발송할 학생을 최소 1명 이상 선택해야 합니다."),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLLCALL_404_1", "문자 템플릿을 찾을 수 없습니다."),
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLLCALL_404_2", "강의를 찾을 수 없습니다."),

    TEMPLATE_STATUS_CONFLICT(HttpStatus.CONFLICT, "ROLLCALL_409_1", "이미 해당 출결 상태의 템플릿이 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
