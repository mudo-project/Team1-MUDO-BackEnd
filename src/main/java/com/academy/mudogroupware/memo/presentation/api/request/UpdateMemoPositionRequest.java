package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.UpdateMemoPositionCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateMemoPositionRequest(
        @NotNull Integer positionX,
        @NotNull Integer positionY,
        @NotNull @Positive Integer width,
        @NotNull @Positive Integer height
) {

    public UpdateMemoPositionCommand toCommand(Long memoId, Long userId) {
        return new UpdateMemoPositionCommand(memoId, userId, positionX, positionY, width, height);
    }
}
