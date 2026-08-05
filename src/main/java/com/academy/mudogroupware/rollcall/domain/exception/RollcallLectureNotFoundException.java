package com.academy.mudogroupware.rollcall.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class RollcallLectureNotFoundException extends NotFoundException {

    public RollcallLectureNotFoundException() {
        super(RollcallErrorCode.LECTURE_NOT_FOUND);
    }
}
