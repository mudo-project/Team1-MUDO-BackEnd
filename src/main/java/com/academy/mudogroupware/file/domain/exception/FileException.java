package com.academy.mudogroupware.file.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class FileException extends BusinessException {

    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }
}
