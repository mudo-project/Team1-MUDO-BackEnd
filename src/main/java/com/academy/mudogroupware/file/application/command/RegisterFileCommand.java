package com.academy.mudogroupware.file.application.command;

public record RegisterFileCommand(
        String objectKey,
        String contentType
) {
}
