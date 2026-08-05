package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionResponseCode implements ResponseCode {

    PERMISSION_LIST_RETRIEVED("PERMISSION_200_1", "권한 카탈로그 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
