package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class TimetableSetNotFoundException extends NotFoundException {

    public TimetableSetNotFoundException() {
        super(TimetableErrorCode.SET_NOT_FOUND);
    }
}
