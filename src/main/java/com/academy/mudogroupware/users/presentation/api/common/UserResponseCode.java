package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserResponseCode implements ResponseCode {

    LOGIN_SUCCEEDED("USER_200_1", "로그인에 성공했습니다."),
    LOGOUT_SUCCEEDED("USER_200_2", "로그아웃되었습니다."),
    USER_SEARCHED("USER_200_3", "구성원 검색에 성공했습니다.");

    private final String code;
    private final String message;
}
