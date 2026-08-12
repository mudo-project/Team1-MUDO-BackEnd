package com.academy.mudogroupware.notification.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class NotificationException extends BusinessException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
