package com.academy.mudogroupware.sharedfile.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

// Drive에서 해당 파일·폴더 ID를 404로 응답했을 때(Guard의 경로 추적 중, 또는 다운로드 대상 조회 시) 던진다.
public class SharedFileItemNotFoundException extends NotFoundException {

    public SharedFileItemNotFoundException(String itemId) {
        super(SharedFileErrorCode.ITEM_NOT_FOUND);
        addContext("itemId", itemId);
    }
}
