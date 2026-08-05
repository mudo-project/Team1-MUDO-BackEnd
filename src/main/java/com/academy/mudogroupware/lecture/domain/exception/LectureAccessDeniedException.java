package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;

public class LectureAccessDeniedException extends ForbiddenException {

    public LectureAccessDeniedException() {
        super(LectureErrorCode.ACCESS_DENIED, LectureErrorCode.ACCESS_DENIED.getMessage());
    }
}
