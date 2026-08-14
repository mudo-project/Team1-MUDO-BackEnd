package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class DuplicateStudentInRequestException extends BadRequestException {

    public DuplicateStudentInRequestException() {
        super(RollcallErrorCode.DUPLICATE_STUDENT_IN_REQUEST);
    }
}
