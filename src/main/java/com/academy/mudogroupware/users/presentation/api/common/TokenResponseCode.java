package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenResponseCode implements ResponseCode {

    ACCESS_TOKEN_REISSUED("TOKEN_200_1", "액세스 토큰이 재발급되었습니다.");

    private final String code;
    private final String message;
}
