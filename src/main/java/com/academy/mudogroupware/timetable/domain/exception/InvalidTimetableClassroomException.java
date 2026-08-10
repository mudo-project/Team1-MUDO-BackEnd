package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTimetableClassroomException extends BadRequestException {

    public InvalidTimetableClassroomException(String field) {
        super(TimetableErrorCode.INVALID_CLASSROOM);
        addContext("field", field);
    }
}
