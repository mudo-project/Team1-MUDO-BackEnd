package com.academy.mudogroupware.users.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMyPasswordRequest(
        @Schema(description = "현재 비밀번호")
        @NotBlank @Size(max = 100) String currentPassword,
        @Schema(description = "새 비밀번호(8자 이상)")
        @NotBlank @Size(min = 8, max = 100) String newPassword) {
}
