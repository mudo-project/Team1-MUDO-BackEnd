package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.CreateRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @Schema(description = "역할 이름. 같은 학원 내 중복 불가", example = "강사")
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "역할 설명", example = "수업 담당")
        @Size(max = 255) String description,
        @Schema(description = "역할 뱃지 색상. 형식 검증 없이 그대로 저장/반환(프론트 책임)", example = "#FF5733")
        @Size(max = 20) String color
) {

    public CreateRoleCommand toCommand() {
        return new CreateRoleCommand(name, description, color);
    }
}
