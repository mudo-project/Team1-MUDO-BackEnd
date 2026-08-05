package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;

public interface CreateLectureUseCase {

    Long createLecture(CreateLectureCommand command);
}
