package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserResponseCode implements ResponseCode {

    LOGIN_SUCCEEDED("USER_200_1", "로그인에 성공했습니다.");

    private final String code;
    private final String message;
}
