package com.academy.mudogroupware.file.application.command;

public record GeneratePresignedUploadUrlCommand(
        Long academyId,
        String fileName,
        String contentType
) {
}
