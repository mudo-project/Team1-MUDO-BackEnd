package com.academy.mudogroupware.lecture.application.usecase;

import com.academy.mudogroupware.lecture.application.command.UpdateLectureCommand;

public interface UpdateLectureUseCase {

    void updateLecture(UpdateLectureCommand command);
}
