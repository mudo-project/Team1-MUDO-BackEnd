package com.academy.mudogroupware.file.presentation.api.response;

public record RegisterFileResponse(
        Long fileId
) {

    public static RegisterFileResponse from(Long fileId) {
        return new RegisterFileResponse(fileId);
    }
}
