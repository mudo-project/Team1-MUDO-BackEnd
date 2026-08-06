package com.academy.mudogroupware.rollcall.application.usecase;

import java.time.LocalDate;

import com.academy.mudogroupware.rollcall.application.query.RosterView;

public interface GetLectureRosterUseCase {

    RosterView getRoster(Long lectureId, Long academyId, LocalDate date);
}
