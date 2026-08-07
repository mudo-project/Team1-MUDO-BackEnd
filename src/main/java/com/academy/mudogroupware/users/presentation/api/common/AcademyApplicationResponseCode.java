package com.academy.mudogroupware.users.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcademyApplicationResponseCode implements ResponseCode {

    ACADEMY_APPLICATION_LIST_FOUND("ACADEMY_APPLICATION_200_1", "학원 신청 목록 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
