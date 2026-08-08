package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class RoleNotFoundException extends NotFoundException {

    public RoleNotFoundException() {
        super(UserErrorCode.ROLE_NOT_FOUND);
    }

    public RoleNotFoundException(Throwable cause) {
        super(UserErrorCode.ROLE_NOT_FOUND, UserErrorCode.ROLE_NOT_FOUND.getMessage(), cause);
    }
}
