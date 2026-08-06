package com.academy.mudogroupware.google.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoogleErrorCode implements ErrorCode {

    OAUTH_STATE_INVALID(
            HttpStatus.BAD_REQUEST,
            "GOOGLE_400_1",
            "구글 인증 요청이 만료되었거나 위조되었습니다. 다시 시도해 주세요."
    ),

    ACCOUNT_NOT_CONNECTED(
            HttpStatus.NOT_FOUND,
            "GOOGLE_404_1",
            "연결된 구글 계정이 없습니다."
    ),

    OAUTH_EXCHANGE_FAILED(
            HttpStatus.BAD_GATEWAY,
            "GOOGLE_502_1",
            "구글 인증 처리 중 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
