package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class PasswordSetupFailedException extends BadRequestException {

    public PasswordSetupFailedException() {
        super(UserErrorCode.PASSWORD_SETUP_FAILED);
    }
}
