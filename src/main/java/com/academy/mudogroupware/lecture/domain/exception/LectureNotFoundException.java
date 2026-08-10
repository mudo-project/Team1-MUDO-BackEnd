package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class LectureNotFoundException extends NotFoundException {

    public LectureNotFoundException() {
        super(LectureErrorCode.LECTURE_NOT_FOUND);
    }
}
