package com.academy.mudogroupware.planquota.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanLimitErrorCode implements ErrorCode {

    EMPLOYEE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_1", "직원 수 한도를 초과하였습니다."),
    STUDENT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_2", "학생 수 한도를 초과하였습니다."),
    RDS_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_3", "저장 공간 한도를 초과하였습니다."),
    S3_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_4", "파일 저장 용량 한도를 초과하였습니다."),
    SMS_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_5", "SMS 발송 한도를 초과하였습니다."),
    AI_TOKEN_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_6", "AI 토큰 사용 한도를 초과하였습니다."),
    MAIL_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "PLANLIMIT_429_7", "메일 발송 한도를 초과하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
