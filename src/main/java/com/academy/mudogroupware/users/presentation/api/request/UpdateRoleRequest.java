package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.UpdateRoleCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        @Size(max = 20) String color
) {

    public UpdateRoleCommand toCommand(Long roleId, Long academyId) {
        return new UpdateRoleCommand(roleId, academyId, name, description, color);
    }
}
