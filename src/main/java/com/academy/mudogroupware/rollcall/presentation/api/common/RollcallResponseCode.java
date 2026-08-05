package com.academy.mudogroupware.rollcall.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RollcallResponseCode implements ResponseCode {

    ROSTER_RETRIEVED("ROLLCALL_200_1", "강의 출결부 조회에 성공했습니다."),
    ATTENDANCE_SAVED("ROLLCALL_200_2", "출결 저장에 성공했습니다."),
    MESSAGE_CANDIDATES_RETRIEVED("ROLLCALL_200_3", "발송 대상 조회에 성공했습니다."),
    TEMPLATE_LIST_RETRIEVED("ROLLCALL_200_4", "문자 템플릿 목록 조회에 성공했습니다."),
    TEMPLATE_CREATED("ROLLCALL_201_1", "문자 템플릿 생성에 성공했습니다.");

    private final String code;
    private final String message;
}
