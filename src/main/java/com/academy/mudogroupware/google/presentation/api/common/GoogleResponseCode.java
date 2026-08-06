package com.academy.mudogroupware.google.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoogleResponseCode implements ResponseCode {

    AUTHORIZATION_URL_ISSUED("GOOGLE_200_1", "구글 인증 URL 발급에 성공했습니다."),
    CONNECTION_RETRIEVED("GOOGLE_200_2", "구글 연동 상태 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
