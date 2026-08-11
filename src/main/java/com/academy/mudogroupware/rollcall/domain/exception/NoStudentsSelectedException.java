package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class NoStudentsSelectedException extends BadRequestException {

    public NoStudentsSelectedException() {
        super(RollcallErrorCode.NO_STUDENTS_SELECTED);
    }
}
