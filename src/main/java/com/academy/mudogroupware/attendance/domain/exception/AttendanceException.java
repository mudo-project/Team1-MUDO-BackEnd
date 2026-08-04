package com.academy.mudogroupware.attendance.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class AttendanceException extends BusinessException {

    public AttendanceException(AttendanceErrorCode errorCode) {
        super(errorCode);
    }
}
