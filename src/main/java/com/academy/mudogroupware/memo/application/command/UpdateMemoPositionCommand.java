package com.academy.mudogroupware.memo.application.command;

public record UpdateMemoPositionCommand(
        Long memoId,
        Long userId,
        int positionX,
        int positionY,
        int width,
        int height
) {
}
