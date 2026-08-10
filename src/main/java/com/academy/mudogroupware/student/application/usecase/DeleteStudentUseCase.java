package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.student.application.command.DeleteStudentCommand;

public interface DeleteStudentUseCase {

    void deleteStudent(DeleteStudentCommand command);
}
