package com.academy.mudogroupware.memo.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemoErrorCode implements ErrorCode {

    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "MEMO_400_1", "제목은 비어 있을 수 없습니다."),
    COLOR_REQUIRED(HttpStatus.BAD_REQUEST, "MEMO_400_2", "색상을 지정해야 합니다."),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMO_400_3", "제목은 100자를 초과할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
