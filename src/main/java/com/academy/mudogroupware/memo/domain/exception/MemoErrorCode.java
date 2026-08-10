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
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "MEMO_400_3", "제목은 100자를 초과할 수 없습니다."),
    MEMO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "MEMO_400_4", "메모는 최대 200개까지 만들 수 있습니다."),

    NOT_MEMO_OWNER(HttpStatus.FORBIDDEN, "MEMO_403_1", "본인의 메모가 아닙니다."),

    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_404_1", "메모를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
