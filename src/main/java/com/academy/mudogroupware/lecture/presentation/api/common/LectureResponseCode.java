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
    LECTURE_UPDATED("LECTURE_200_3", "강의 수정에 성공했습니다."),
    LECTURE_TEACHER_NAMES_RETRIEVED("LECTURE_200_4", "강의 담당 선생님 목록 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
