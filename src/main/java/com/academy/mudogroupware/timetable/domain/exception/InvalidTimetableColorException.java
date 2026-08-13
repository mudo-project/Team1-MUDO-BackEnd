package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTimetableColorException extends BadRequestException {

    public InvalidTimetableColorException() {
        super(TimetableErrorCode.INVALID_COLOR);
    }
}
