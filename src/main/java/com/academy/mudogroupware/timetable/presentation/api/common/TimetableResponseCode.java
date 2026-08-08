package com.academy.mudogroupware.timetable.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimetableResponseCode implements ResponseCode {

    SET_CREATED("TIMETABLE_201_1", "시간표 세트 생성에 성공했습니다.");

    private final String code;
    private final String message;
}
