package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class AcademyApplicationAlreadyReviewedException extends ConflictException {

    public AcademyApplicationAlreadyReviewedException() {
        super(UserErrorCode.ACADEMY_APPLICATION_ALREADY_REVIEWED);
    }
}
