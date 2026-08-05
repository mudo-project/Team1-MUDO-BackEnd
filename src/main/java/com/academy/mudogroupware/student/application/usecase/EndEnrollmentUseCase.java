package com.academy.mudogroupware.student.application.usecase;

import com.academy.mudogroupware.student.application.command.EndEnrollmentCommand;

public interface EndEnrollmentUseCase {

    void end(EndEnrollmentCommand command);
}
