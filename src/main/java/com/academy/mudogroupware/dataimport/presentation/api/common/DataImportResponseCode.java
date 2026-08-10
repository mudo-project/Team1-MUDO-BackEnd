package com.academy.mudogroupware.dataimport.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DataImportResponseCode implements ResponseCode {

    DRAFT_RETRIEVED("DATA_IMPORT_200_1", "가져오기 초안 조회에 성공했습니다."),
    DRAFT_UPDATED("DATA_IMPORT_200_2", "가져오기 초안 수정에 성공했습니다."),
    IMPORT_CONFIRMED("DATA_IMPORT_200_3", "가져오기 확정에 성공했습니다."),
    RESULT_RETRIEVED("DATA_IMPORT_200_4", "가져오기 결과 조회에 성공했습니다."),
    IMPORT_CREATED("DATA_IMPORT_201_1", "가져오기 작업 생성에 성공했습니다.");

    private final String code;
    private final String message;
}
