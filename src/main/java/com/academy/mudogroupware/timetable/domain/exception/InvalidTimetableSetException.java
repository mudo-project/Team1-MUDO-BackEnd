package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTimetableSetException extends BadRequestException {

    public InvalidTimetableSetException(String field) {
        super(TimetableErrorCode.INVALID_SET_CONFIGURATION);
        addContext("field", field);
    }
}
