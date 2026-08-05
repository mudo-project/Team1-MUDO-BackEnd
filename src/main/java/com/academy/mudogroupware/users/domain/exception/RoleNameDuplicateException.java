package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class RoleNameDuplicateException extends ConflictException {

    public RoleNameDuplicateException() {
        super(UserErrorCode.ROLE_NAME_DUPLICATE);
    }
}
