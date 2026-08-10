package com.academy.mudogroupware.memo.application.usecase;

import com.academy.mudogroupware.memo.application.command.UpdateMemoPositionCommand;

public interface UpdateMemoPositionUseCase {

    void updatePosition(UpdateMemoPositionCommand command);
}
