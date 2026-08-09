package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class UsernameDuplicateException extends ConflictException {

    public UsernameDuplicateException() {
        super(UserErrorCode.USERNAME_DUPLICATE);
    }

    public UsernameDuplicateException(Throwable cause) {
        super(UserErrorCode.USERNAME_DUPLICATE, UserErrorCode.USERNAME_DUPLICATE.getMessage(), cause);
    }
}
