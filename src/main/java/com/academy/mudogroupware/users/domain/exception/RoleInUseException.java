package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class RoleInUseException extends ConflictException {

    public RoleInUseException() {
        super(UserErrorCode.ROLE_IN_USE);
    }
}
