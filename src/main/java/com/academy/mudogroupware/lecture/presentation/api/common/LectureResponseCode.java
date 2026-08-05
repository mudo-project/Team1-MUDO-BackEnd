package com.academy.mudogroupware.lecture.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LectureResponseCode implements ResponseCode {

    LECTURE_CREATED("LECTURE_201_1", "강의 등록에 성공했습니다."),
    LECTURE_LIST_RETRIEVED("LECTURE_200_1", "강의 목록 조회에 성공했습니다."),
    LECTURE_DETAIL_RETRIEVED("LECTURE_200_2", "강의 상세 조회에 성공했습니다."),
    STUDENT_REGISTERED("LECTURE_201_2", "학생 등록에 성공했습니다.");

    private final String code;
    private final String message;
}
