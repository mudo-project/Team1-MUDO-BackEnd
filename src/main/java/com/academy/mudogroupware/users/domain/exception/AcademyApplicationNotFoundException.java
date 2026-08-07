package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class AcademyApplicationNotFoundException extends NotFoundException {

    public AcademyApplicationNotFoundException() {
        super(UserErrorCode.ACADEMY_APPLICATION_NOT_FOUND);
    }
}
