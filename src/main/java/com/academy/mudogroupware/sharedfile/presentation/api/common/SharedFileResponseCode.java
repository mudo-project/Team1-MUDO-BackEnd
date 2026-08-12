package com.academy.mudogroupware.sharedfile.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SharedFileResponseCode implements ResponseCode {

    ROOT_RETRIEVED("SHAREDFILE_200_1", "시스템 루트 상태 조회에 성공했습니다."),
    ROOT_RECREATED("SHAREDFILE_200_2", "시스템 루트 재생성에 성공했습니다."),
    ITEMS_LISTED("SHAREDFILE_200_3", "폴더 목록 조회에 성공했습니다."),
    ITEM_RETRIEVED("SHAREDFILE_200_4", "파일·폴더 상세 조회에 성공했습니다."),
    ITEMS_SEARCHED("SHAREDFILE_200_5", "검색에 성공했습니다."),
    ITEM_UPDATED("SHAREDFILE_200_6", "이름 변경·이동에 성공했습니다."),
    FOLDER_CREATED("SHAREDFILE_201_1", "폴더 생성에 성공했습니다."),
    FILE_UPLOADED("SHAREDFILE_201_2", "파일 업로드에 성공했습니다."),
    GOOGLE_FILE_CREATED("SHAREDFILE_201_3", "Google 파일 생성에 성공했습니다.");

    private final String code;
    private final String message;
}
