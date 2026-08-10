package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTimetableSlotException extends BadRequestException {

    public InvalidTimetableSlotException(String field) {
        super(TimetableErrorCode.INVALID_SLOT_CONFIGURATION);
        addContext("field", field);
    }
}
