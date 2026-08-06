package com.academy.mudogroupware.lecture.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class LectureScheduleRequiredException extends BadRequestException {

    public LectureScheduleRequiredException() {
        super(LectureErrorCode.SCHEDULE_REQUIRED);
    }
}
