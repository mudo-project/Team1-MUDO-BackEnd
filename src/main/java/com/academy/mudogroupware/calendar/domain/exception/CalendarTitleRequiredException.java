package com.academy.mudogroupware.calendar.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class CalendarTitleRequiredException extends BadRequestException {

    public CalendarTitleRequiredException() {
        super(CalendarErrorCode.TITLE_REQUIRED);
    }
}
