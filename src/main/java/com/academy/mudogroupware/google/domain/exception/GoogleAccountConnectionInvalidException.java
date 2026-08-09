package com.academy.mudogroupware.google.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class GoogleAccountConnectionInvalidException extends ConflictException {

    public GoogleAccountConnectionInvalidException() {
        super(GoogleErrorCode.ACCOUNT_CONNECTION_INVALID);
    }
}
