package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleResponseCode implements ResponseCode {

    ROLE_CREATED("ROLE_201_1", "역할 생성에 성공했습니다."),
    ROLE_LIST_FOUND("ROLE_200_1", "역할 목록 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
