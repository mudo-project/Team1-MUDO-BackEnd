package com.academy.mudogroupware.memo.application.usecase;

import com.academy.mudogroupware.memo.application.command.DeleteMemoCommand;

public interface DeleteMemoUseCase {

    void deleteMemo(DeleteMemoCommand command);
}
