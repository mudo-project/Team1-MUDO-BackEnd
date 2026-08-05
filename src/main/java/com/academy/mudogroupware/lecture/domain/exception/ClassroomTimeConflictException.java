package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class ClassroomTimeConflictException extends ConflictException {

    public ClassroomTimeConflictException() {
        super(LectureErrorCode.CLASSROOM_TIME_CONFLICT);
    }
}
