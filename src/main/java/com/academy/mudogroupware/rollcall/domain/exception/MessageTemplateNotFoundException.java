package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class MessageTemplateNotFoundException extends NotFoundException {

    public MessageTemplateNotFoundException() {
        super(RollcallErrorCode.TEMPLATE_NOT_FOUND);
    }
}
