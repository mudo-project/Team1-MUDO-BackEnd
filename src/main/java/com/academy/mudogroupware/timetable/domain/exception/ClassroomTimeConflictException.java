package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class ClassroomTimeConflictException extends ConflictException {

    public ClassroomTimeConflictException() {
        super(TimetableErrorCode.CLASSROOM_TIME_CONFLICT);
    }
}
