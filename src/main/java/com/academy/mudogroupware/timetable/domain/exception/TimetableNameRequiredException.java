package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class TimetableNameRequiredException extends BadRequestException {

    public TimetableNameRequiredException() {
        super(TimetableErrorCode.NAME_REQUIRED);
    }
}
