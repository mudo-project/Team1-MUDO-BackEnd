package com.academy.mudogroupware.notice.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeResponseCode implements ResponseCode {

    NOTICE_CREATED("NOTICE_201_1", "공지사항 작성에 성공했습니다."),
    NOTICE_LIST_RETRIEVED("NOTICE_200_1", "공지사항 목록 조회에 성공했습니다."),
    NOTICE_DETAIL_RETRIEVED("NOTICE_200_2", "공지사항 상세 조회에 성공했습니다."),
    NOTICE_READERS_RETRIEVED("NOTICE_200_3", "공지사항 읽은 사람 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
