package com.academy.mudogroupware.file.application.command;

public record GeneratePresignedUploadUrlCommand(
        String fileName,
        String contentType
) {
}
