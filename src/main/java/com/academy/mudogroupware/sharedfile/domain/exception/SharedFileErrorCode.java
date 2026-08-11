package com.academy.mudogroupware.sharedfile.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 공유파일 도메인 오류코드. SHAREDFILE_409_2(Google 계정 미연결·만료)는 Task4에서
// GetGoogleAccessTokenUseCase 예외를 감싸는 시점에 추가할 예정이라 아직 없다.
@Getter
@RequiredArgsConstructor
public enum SharedFileErrorCode implements ErrorCode {

    INVALID_NAME(
            HttpStatus.BAD_REQUEST,
            "SHAREDFILE_400_1",
            "이름이 올바르지 않습니다."
    ),

    UPLOAD_TOO_LARGE(
            HttpStatus.BAD_REQUEST,
            "SHAREDFILE_400_2",
            "파일은 최대 100MB까지 업로드할 수 있습니다."
    ),

    INVALID_EXPORT_FORMAT(
            HttpStatus.BAD_REQUEST,
            "SHAREDFILE_400_3",
            "지원하지 않는 다운로드 형식입니다."
    ),

    OUT_OF_ROOT(
            HttpStatus.FORBIDDEN,
            "SHAREDFILE_403_1",
            "시스템 루트 하위 항목만 조회·변경할 수 있습니다."
    ),

    ITEM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SHAREDFILE_404_1",
            "파일 또는 폴더를 찾을 수 없습니다."
    ),

    ROOT_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "SHAREDFILE_409_1",
            "공유파일 시스템 루트가 아직 생성되지 않았거나 사용할 수 없습니다."
    ),

    DRIVE_FAILURE(
            HttpStatus.BAD_GATEWAY,
            "SHAREDFILE_502_1",
            "Google Drive 처리 중 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
