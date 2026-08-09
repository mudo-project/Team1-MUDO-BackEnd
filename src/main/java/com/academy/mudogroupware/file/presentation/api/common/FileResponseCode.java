package com.academy.mudogroupware.file.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileResponseCode implements ResponseCode {

    PRESIGNED_URL_GENERATED("FILE_200_1", "업로드용 URL 발급에 성공했습니다."),
    FILE_REGISTERED("FILE_201_1", "파일 등록에 성공했습니다."),
    DOWNLOAD_URL_RETRIEVED("FILE_200_2", "다운로드용 URL 조회에 성공했습니다.");

    private final String code;
    private final String message;
}
