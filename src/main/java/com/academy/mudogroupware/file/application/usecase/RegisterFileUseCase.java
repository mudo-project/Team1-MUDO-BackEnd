package com.academy.mudogroupware.file.application.usecase;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;

public interface RegisterFileUseCase {

    Long register(RegisterFileCommand command);
}
