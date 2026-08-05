package com.academy.mudogroupware.student.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentErrorCode implements ErrorCode {

    STUDENT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_400_1", "학생 이름은 비어 있을 수 없습니다."),
    STUDENT_GRADE_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_400_2", "학생 학년은 필수입니다."),
    LECTURE_REQUIRED(HttpStatus.BAD_REQUEST, "STUDENT_400_3", "등록할 강의는 필수입니다."),

    STUDENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STUDENT_403_1", "해당 학생에 접근할 권한이 없습니다."),

    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDENT_404_1", "학생을 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDENT_404_2", "수강 등록 정보를 찾을 수 없습니다."),

    DUPLICATE_ACTIVE_ENROLLMENT(HttpStatus.CONFLICT, "STUDENT_409_1", "이미 수강 중인 강의입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
