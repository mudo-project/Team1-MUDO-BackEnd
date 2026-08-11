package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

// 업로드 파일이 100MB를 초과할 때 던진다(Task4의 UploadSharedFileUseCase에서 사용).
public class SharedFileUploadTooLargeException extends BadRequestException {

    public SharedFileUploadTooLargeException() {
        super(SharedFileErrorCode.UPLOAD_TOO_LARGE);
    }
}
