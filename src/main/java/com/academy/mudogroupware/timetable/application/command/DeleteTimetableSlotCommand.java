package com.academy.mudogroupware.timetable.application.command;

import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

public record DeleteTimetableSlotCommand(Long academyId, Long timetableSetId, Long timetableSlotId, UpdateScope scope) {
}
