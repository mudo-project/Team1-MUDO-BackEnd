package com.academy.mudogroupware.memo.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoResponseCode implements ResponseCode {

    MEMO_CREATED("MEMO_201_1", "메모 생성에 성공했습니다."),
    MEMO_LIST_RETRIEVED("MEMO_200_1", "메모 목록 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
