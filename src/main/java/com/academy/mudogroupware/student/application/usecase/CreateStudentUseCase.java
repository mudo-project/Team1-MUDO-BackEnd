package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.student.application.command.CreateStudentCommand;

public interface CreateStudentUseCase {

    Long createStudent(CreateStudentCommand command);
}
