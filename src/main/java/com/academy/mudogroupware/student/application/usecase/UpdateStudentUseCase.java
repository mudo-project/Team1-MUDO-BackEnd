package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.student.application.command.UpdateStudentCommand;

public interface UpdateStudentUseCase {

    void updateStudent(UpdateStudentCommand command);
}
