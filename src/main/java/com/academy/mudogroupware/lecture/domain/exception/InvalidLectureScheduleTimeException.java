package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidLectureScheduleTimeException extends BadRequestException {

    public InvalidLectureScheduleTimeException() {
        super(LectureErrorCode.INVALID_SCHEDULE_TIME);
    }
}
