package com.academy.mudogroupware.lecture.application.command;

public record DeleteLectureCommand(
        Long lectureId,
        Long requesterId
) {
}
