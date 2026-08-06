package com.academy.mudogroupware.calendar.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CalendarResponseCode implements ResponseCode {

    EVENT_CREATED("CALENDAR_201_1", "일정 생성에 성공했습니다."),
    EVENT_LIST_RETRIEVED("CALENDAR_200_1", "일정 목록 조회에 성공했습니다."),
    EVENT_DETAIL_RETRIEVED("CALENDAR_200_2", "일정 상세 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
