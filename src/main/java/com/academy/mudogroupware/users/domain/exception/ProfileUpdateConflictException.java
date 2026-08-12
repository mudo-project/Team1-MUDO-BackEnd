package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class ProfileUpdateConflictException extends ConflictException {

    public ProfileUpdateConflictException(Throwable cause) {
        super(UserErrorCode.PROFILE_UPDATE_CONFLICT, UserErrorCode.PROFILE_UPDATE_CONFLICT.getMessage(), cause);
    }
}
