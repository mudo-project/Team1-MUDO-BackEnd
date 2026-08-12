package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.domain.model.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
        @Schema(description = "변경할 재직 상태: ACTIVE/RESIGNED/INACTIVE", example = "RESIGNED")
        @NotNull UserStatus status) {
}
