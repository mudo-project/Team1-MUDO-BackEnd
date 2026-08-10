package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;

public interface EnrollStudentUseCase {

    Long enroll(EnrollStudentCommand command);
}
