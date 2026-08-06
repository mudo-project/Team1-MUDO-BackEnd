package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class CalendarEventNotFoundException extends NotFoundException {

    public CalendarEventNotFoundException(Long eventId) {
        super(CalendarErrorCode.EVENT_NOT_FOUND);
        addContext("eventId", eventId);
    }
}
