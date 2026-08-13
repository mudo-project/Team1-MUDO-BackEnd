package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserResponseCode implements ResponseCode {

    LOGIN_SUCCEEDED("USER_200_1", "로그인에 성공했습니다."),
    LOGOUT_SUCCEEDED("USER_200_2", "로그아웃되었습니다."),
    USER_SEARCHED("USER_200_3", "구성원 검색에 성공했습니다."),
    MEMBERS_LISTED("USER_200_4", "구성원 목록 조회에 성공했습니다."),
    MY_PROFILE_RETRIEVED("USER_200_5", "내 정보 조회에 성공했습니다."),
    MEMBER_DETAIL_RETRIEVED("USER_200_6", "구성원 상세 조회에 성공했습니다."),
    MY_PERMISSIONS_RETRIEVED("USER_200_7", "내 권한 목록 조회에 성공했습니다."),
    ACCOUNT_CREATED("USER_201_1", "직원 계정이 발급되었습니다.");

    private final String code;
    private final String message;
}
