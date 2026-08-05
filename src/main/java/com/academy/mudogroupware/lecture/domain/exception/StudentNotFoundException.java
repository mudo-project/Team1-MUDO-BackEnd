package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class StudentNotFoundException extends NotFoundException {

    public StudentNotFoundException() {
        super(LectureErrorCode.STUDENT_NOT_FOUND);
    }
}
