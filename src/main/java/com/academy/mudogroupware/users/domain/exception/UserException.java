package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class UserException extends BusinessException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
