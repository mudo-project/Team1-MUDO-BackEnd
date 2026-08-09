package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class TimetableSlotNotFoundException extends NotFoundException {

    public TimetableSlotNotFoundException() {
        super(TimetableErrorCode.SLOT_NOT_FOUND);
    }
}
