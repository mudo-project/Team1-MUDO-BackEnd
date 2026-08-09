package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class DuplicateClassroomCodeException extends BadRequestException {

    public DuplicateClassroomCodeException() {
        super(TimetableErrorCode.DUPLICATE_CLASSROOM_CODE);
    }
}
