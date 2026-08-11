package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

// 폴더·파일 이름이 비어있거나 유효하지 않을 때 던진다(Task4~5의 생성·이름변경 UseCase에서 사용).
public class SharedFileInvalidNameException extends BadRequestException {

    public SharedFileInvalidNameException() {
        super(SharedFileErrorCode.INVALID_NAME);
    }
}
