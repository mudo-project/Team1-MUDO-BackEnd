package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.InfrastructureException;

// GoogleDriveAdapter가 404가 아닌 오류(401·403·5xx·네트워크 오류)를 만났을 때 던진다.
// 루트 상태는 바꾸지 않고 해당 요청만 실패로 처리하기 위한 오류다.
public class SharedFileDriveFailureException extends InfrastructureException {

    public SharedFileDriveFailureException(Throwable cause) {
        super(SharedFileErrorCode.DRIVE_FAILURE, SharedFileErrorCode.DRIVE_FAILURE.getMessage(), cause);
    }
}
