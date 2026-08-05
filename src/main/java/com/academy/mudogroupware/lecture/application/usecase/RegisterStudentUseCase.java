package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.lecture.application.command.RegisterStudentCommand;

public interface RegisterStudentUseCase {

    Long registerStudent(RegisterStudentCommand command);
}
