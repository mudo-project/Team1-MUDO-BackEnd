package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class CalendarEventUpdateConflictException extends ConflictException {

    public CalendarEventUpdateConflictException(Throwable cause) {
        super(CalendarErrorCode.EVENT_UPDATE_CONFLICT, CalendarErrorCode.EVENT_UPDATE_CONFLICT.getMessage(), cause);
    }
}
