package com.academy.mudogroupware.messenger.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class MessengerException extends BusinessException {

    public MessengerException(MessengerErrorCode errorCode) {
        super(errorCode);
    }
}
