package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

// Docs/Sheets/Slides 각각에서 지원하지 않는 다운로드 형식을 요청했을 때 던진다(Task5의 다운로드 UseCase에서 사용).
public class SharedFileInvalidExportFormatException extends BadRequestException {

    public SharedFileInvalidExportFormatException() {
        super(SharedFileErrorCode.INVALID_EXPORT_FORMAT);
    }
}
