package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class TimetableSetUpdateConflictException extends ConflictException {

    public TimetableSetUpdateConflictException(Throwable cause) {
        super(TimetableErrorCode.SET_UPDATE_CONFLICT, TimetableErrorCode.SET_UPDATE_CONFLICT.getMessage(), cause);
    }
}
