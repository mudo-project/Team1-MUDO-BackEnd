package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class TimetableSlotUpdateConflictException extends ConflictException {

    public TimetableSlotUpdateConflictException(Throwable cause) {
        super(TimetableErrorCode.SLOT_UPDATE_CONFLICT, TimetableErrorCode.SLOT_UPDATE_CONFLICT.getMessage(), cause);
    }
}
