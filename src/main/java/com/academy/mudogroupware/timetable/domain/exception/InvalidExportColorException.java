package com.academy.mudogroupware.timetable.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidExportColorException extends BadRequestException {

    public InvalidExportColorException() {
        super(TimetableErrorCode.INVALID_EXPORT_COLOR);
    }

    public InvalidExportColorException(Throwable cause) {
        super(TimetableErrorCode.INVALID_EXPORT_COLOR, TimetableErrorCode.INVALID_EXPORT_COLOR.getMessage(), cause);
    }
}
