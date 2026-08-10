package com.academy.mudogroupware.approval.application.port;

public class AttachmentContentUnavailableException extends RuntimeException {

    public AttachmentContentUnavailableException(Long fileId) {
        super("첨부파일 원문을 조회할 수 없습니다. fileId=" + fileId);
    }
}
