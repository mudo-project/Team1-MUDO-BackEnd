package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.CreateRoleCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        String color
) {

    public CreateRoleCommand toCommand(Long academyId) {
        return new CreateRoleCommand(academyId, name, description, color);
    }
}
