package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class RoleNotFoundException extends NotFoundException {

    public RoleNotFoundException() {
        super(UserErrorCode.ROLE_NOT_FOUND);
    }
}
