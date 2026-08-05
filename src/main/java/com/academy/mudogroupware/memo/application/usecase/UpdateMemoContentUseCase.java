package com.academy.mudogroupware.memo.application.usecase;

import com.academy.mudogroupware.memo.application.command.UpdateMemoContentCommand;

public interface UpdateMemoContentUseCase {

    void updateContent(UpdateMemoContentCommand command);
}
