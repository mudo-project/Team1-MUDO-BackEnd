package com.academy.mudogroupware.memo.application.usecase;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;

public interface CreateMemoUseCase {

    Long createMemo(CreateMemoCommand command);
}
