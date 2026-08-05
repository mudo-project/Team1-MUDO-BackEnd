package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import java.util.Set;

public class InvalidPermissionCodeException extends BadRequestException {

    public InvalidPermissionCodeException(Set<String> missingCodes) {
        super(UserErrorCode.INVALID_PERMISSION_CODE);
        addContext("missingCodes", missingCodes);
    }
}
