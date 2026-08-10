package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.UpdateRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @Schema(description = "역할 이름. 같은 학원 내 자기 자신을 제외하고 중복 불가", example = "수석 강사")
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "역할 설명", example = "고급반 수업 담당")
        @Size(max = 255) String description,
        @Schema(description = "역할 뱃지 색상. 형식 검증 없이 그대로 저장/반환(프론트 책임)", example = "#FF5733")
        @Size(max = 20) String color
) {

    public UpdateRoleCommand toCommand(Long roleId, Long academyId) {
        return new UpdateRoleCommand(roleId, academyId, name, description, color);
    }
}
