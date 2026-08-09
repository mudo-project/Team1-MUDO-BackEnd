package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class UnsupportedSlotScopeException extends BadRequestException {

    public UnsupportedSlotScopeException() {
        super(TimetableErrorCode.UNSUPPORTED_SCOPE);
    }
}
