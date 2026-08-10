package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTimetablePeriodException extends BadRequestException {

    public InvalidTimetablePeriodException() {
        super(TimetableErrorCode.INVALID_PERIOD);
    }
}
