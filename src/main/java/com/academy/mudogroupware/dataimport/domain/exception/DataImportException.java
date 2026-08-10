package com.academy.mudogroupware.dataimport.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class DataImportException extends BusinessException {

    public DataImportException(DataImportErrorCode errorCode) {
        super(errorCode);
    }
}
