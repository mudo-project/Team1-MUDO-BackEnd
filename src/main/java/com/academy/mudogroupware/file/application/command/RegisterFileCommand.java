package com.academy.mudogroupware.file.application.command;

public record RegisterFileCommand(
        Long academyId,
        String objectKey,
        String contentType
) {
}
