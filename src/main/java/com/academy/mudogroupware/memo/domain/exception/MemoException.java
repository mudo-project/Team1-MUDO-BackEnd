package com.academy.mudogroupware.memo.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class MemoException extends BusinessException {

    public MemoException(MemoErrorCode errorCode) {
        super(errorCode);
    }
}
