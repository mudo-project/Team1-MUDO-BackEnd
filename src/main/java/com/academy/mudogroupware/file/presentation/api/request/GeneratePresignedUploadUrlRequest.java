package com.academy.mudogroupware.file.presentation.api.request;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GeneratePresignedUploadUrlRequest(
        @Schema(description = "원본 파일명", example = "휴가원.pdf")
        @NotBlank String fileName,

        @Schema(description = "MIME 타입", example = "application/pdf")
        @NotBlank String contentType
) {

    public GeneratePresignedUploadUrlCommand toCommand() {
        return new GeneratePresignedUploadUrlCommand(fileName, contentType);
    }
}
