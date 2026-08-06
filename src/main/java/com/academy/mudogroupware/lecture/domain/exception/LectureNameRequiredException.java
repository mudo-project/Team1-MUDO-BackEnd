package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class LectureNameRequiredException extends BadRequestException {

    public LectureNameRequiredException() {
        super(LectureErrorCode.NAME_REQUIRED);
    }
}
