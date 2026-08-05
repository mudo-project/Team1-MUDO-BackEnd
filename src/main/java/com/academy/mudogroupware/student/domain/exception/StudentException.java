package com.academy.mudogroupware.student.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class StudentException extends BusinessException {

    public StudentException(StudentErrorCode errorCode) {
        super(errorCode);
    }
}
