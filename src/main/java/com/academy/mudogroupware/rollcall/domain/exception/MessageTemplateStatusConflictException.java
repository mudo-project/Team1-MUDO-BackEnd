package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class MessageTemplateStatusConflictException extends ConflictException {

    public MessageTemplateStatusConflictException() {
        super(RollcallErrorCode.TEMPLATE_STATUS_CONFLICT);
    }
}
