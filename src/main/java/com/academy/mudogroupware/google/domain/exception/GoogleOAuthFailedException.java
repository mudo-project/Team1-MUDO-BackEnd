package com.academy.mudogroupware.google.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.InfrastructureException;

public class GoogleOAuthFailedException extends InfrastructureException {

    public GoogleOAuthFailedException(Throwable cause) {
        super(GoogleErrorCode.OAUTH_EXCHANGE_FAILED, GoogleErrorCode.OAUTH_EXCHANGE_FAILED.getMessage(), cause);
    }
}
