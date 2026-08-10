package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidCalendarPeriodException extends BadRequestException {

    public InvalidCalendarPeriodException() {
        super(CalendarErrorCode.INVALID_PERIOD);
    }
}
