package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class StudentNotEnrolledException extends BadRequestException {

    public StudentNotEnrolledException() {
        super(RollcallErrorCode.STUDENT_NOT_ENROLLED);
    }
}
