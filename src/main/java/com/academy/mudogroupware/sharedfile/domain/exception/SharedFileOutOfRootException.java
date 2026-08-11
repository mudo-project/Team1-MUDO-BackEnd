package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;

// SharedFileRootGuard가 대상이 시스템 루트 하위가 아니거나, 시스템 루트 자체를 변경 대상으로
// 지정한 것을 발견했을 때 던진다.
public class SharedFileOutOfRootException extends ForbiddenException {

    public SharedFileOutOfRootException(String itemId) {
        super(SharedFileErrorCode.OUT_OF_ROOT, SharedFileErrorCode.OUT_OF_ROOT.getMessage());
        addContext("itemId", itemId);
    }
}
