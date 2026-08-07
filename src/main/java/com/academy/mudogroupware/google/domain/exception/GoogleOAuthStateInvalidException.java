package com.academy.mudogroupware.google.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class GoogleOAuthStateInvalidException extends BadRequestException {

    public GoogleOAuthStateInvalidException() {
        super(GoogleErrorCode.OAUTH_STATE_INVALID);
    }
}
