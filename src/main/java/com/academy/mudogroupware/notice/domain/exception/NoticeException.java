package com.academy.mudogroupware.notice.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class NoticeException extends BusinessException {

    public NoticeException(NoticeErrorCode errorCode) {
        super(errorCode);
    }
}
