package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.lecture.application.command.DeleteLectureCommand;

public interface DeleteLectureUseCase {

    void deleteLecture(DeleteLectureCommand command);
}
