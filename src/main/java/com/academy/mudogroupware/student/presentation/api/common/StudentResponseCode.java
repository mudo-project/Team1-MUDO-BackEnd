package com.academy.mudogroupware.student.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentResponseCode implements ResponseCode {

    STUDENT_CREATED("STUDENT_201_1", "학생 등록에 성공했습니다."),
    ENROLLMENT_CREATED("STUDENT_201_2", "수강 등록에 성공했습니다."),
    STUDENT_LIST_RETRIEVED("STUDENT_200_1", "학생 목록 조회에 성공했습니다."),
    STUDENT_DETAIL_RETRIEVED("STUDENT_200_2", "학생 상세 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
