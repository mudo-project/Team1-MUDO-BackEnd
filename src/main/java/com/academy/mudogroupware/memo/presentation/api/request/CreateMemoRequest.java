package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMemoRequest(
        @NotBlank @Size(max = 100) String title,
        String content,
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{6}$") String color
) {

    public CreateMemoCommand toCommand(Long userId) {
        return new CreateMemoCommand(userId, title, content, color);
    }
}
