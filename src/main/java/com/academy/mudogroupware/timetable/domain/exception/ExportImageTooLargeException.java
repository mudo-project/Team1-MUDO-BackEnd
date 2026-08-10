package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class ExportImageTooLargeException extends BadRequestException {

    public ExportImageTooLargeException() {
        super(TimetableErrorCode.EXPORT_IMAGE_TOO_LARGE);
    }
}
