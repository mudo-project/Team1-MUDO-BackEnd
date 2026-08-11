package com.academy.mudogroupware.file.presentation.api.request;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegisterFileRequest(
        @Schema(description = "presigned URL 발급 시 받은 objectKey", example = "tenants/academy-a/files/3f2c-휴가원.pdf")
        @NotBlank String objectKey,

        @Schema(description = "MIME 타입", example = "application/pdf")
        @NotBlank String contentType
) {

    public RegisterFileCommand toCommand() {
        return new RegisterFileCommand(objectKey, contentType);
    }
}
