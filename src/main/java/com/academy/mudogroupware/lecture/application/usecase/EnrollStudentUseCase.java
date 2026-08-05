package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.lecture.application.command.EnrollStudentCommand;

public interface EnrollStudentUseCase {

    void enrollStudent(EnrollStudentCommand command);
}
