package com.academy.mudogroupware.dataimport.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DataImportErrorCode implements ErrorCode {

    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "DATA_IMPORT_400_1", "업로드할 파일이 필요합니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "DATA_IMPORT_400_2", "지원하지 않는 파일 형식입니다."),

    IMPORT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DATA_IMPORT_403_1", "해당 가져오기 작업에 접근할 권한이 없습니다."),

    IMPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "DATA_IMPORT_404_1", "가져오기 작업을 찾을 수 없습니다."),

    EMPTY_ANALYSIS_RESULT(HttpStatus.CONFLICT, "DATA_IMPORT_409_1", "분석된 등록 후보가 없습니다."),
    IMPORT_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "DATA_IMPORT_409_2", "이미 확정된 가져오기 작업입니다."),
    SELECTED_ROW_NOT_READY(HttpStatus.CONFLICT, "DATA_IMPORT_409_3", "등록 가능한 상태가 아닌 행이 선택되어 있습니다."),
    ENROLLMENT_TARGET_NOT_FOUND(HttpStatus.CONFLICT, "DATA_IMPORT_409_4", "수강 등록에 필요한 학생 또는 강의 후보를 찾을 수 없습니다."),
    RESULT_NOT_AVAILABLE(HttpStatus.CONFLICT, "DATA_IMPORT_409_5", "아직 확정 결과가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
