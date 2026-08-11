package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class EmailDuplicateException extends ConflictException {

    public EmailDuplicateException() {
        super(UserErrorCode.EMAIL_DUPLICATE);
    }

    public EmailDuplicateException(Throwable cause) {
        super(UserErrorCode.EMAIL_DUPLICATE, UserErrorCode.EMAIL_DUPLICATE.getMessage(), cause);
    }
}
