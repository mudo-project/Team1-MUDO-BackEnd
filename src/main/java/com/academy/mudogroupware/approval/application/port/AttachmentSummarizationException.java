package com.academy.mudogroupware.approval.application.port;

public class AttachmentSummarizationException extends RuntimeException {

    public AttachmentSummarizationException(String message) {
        super(message);
    }

    public AttachmentSummarizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
