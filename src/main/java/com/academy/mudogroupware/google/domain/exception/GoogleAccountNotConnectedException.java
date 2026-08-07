package com.academy.mudogroupware.google.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class GoogleAccountNotConnectedException extends NotFoundException {

    public GoogleAccountNotConnectedException() {
        super(GoogleErrorCode.ACCOUNT_NOT_CONNECTED);
    }
}
