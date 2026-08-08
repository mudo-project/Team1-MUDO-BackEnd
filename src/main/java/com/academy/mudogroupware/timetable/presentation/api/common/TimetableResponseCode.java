package com.academy.mudogroupware.timetable.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimetableResponseCode implements ResponseCode {

    SET_CREATED("TIMETABLE_201_1", "시간표 세트 생성에 성공했습니다."),
    SET_LIST_RETRIEVED("TIMETABLE_200_1", "시간표 세트 목록 조회에 성공했습니다."),
    SET_DETAIL_RETRIEVED("TIMETABLE_200_2", "시간표 세트 상세 조회에 성공했습니다."),
    SLOT_CREATED("TIMETABLE_201_2", "수업 슬롯 등록에 성공했습니다."),
    SLOT_LIST_RETRIEVED("TIMETABLE_200_3", "수업 슬롯 목록 조회에 성공했습니다."),
    SLOT_DETAIL_RETRIEVED("TIMETABLE_200_4", "수업 슬롯 상세 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
