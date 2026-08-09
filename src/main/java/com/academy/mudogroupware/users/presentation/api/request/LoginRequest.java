package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.LoginCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "로그인 아이디", example = "kim_teacher01")
        @NotBlank @Size(max = 50) String username,
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank @Size(max = 100) String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
