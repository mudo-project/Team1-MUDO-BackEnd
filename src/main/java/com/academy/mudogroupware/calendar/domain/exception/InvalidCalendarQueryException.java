package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidCalendarQueryException extends BadRequestException {

    public InvalidCalendarQueryException() {
        super(CalendarErrorCode.INVALID_QUERY_RANGE);
    }
}
