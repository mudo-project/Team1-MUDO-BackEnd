package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

// 시스템 루트가 아직 생성되지 않았거나(연동 직후 생성 실패) FAILED 상태일 때, 콘텐츠 조회·변경
// 요청을 거부하기 위해 UseCase가 던진다(Task4~5에서 사용).
public class SharedFileRootUnavailableException extends ConflictException {

    public SharedFileRootUnavailableException() {
        super(SharedFileErrorCode.ROOT_UNAVAILABLE);
    }
}
