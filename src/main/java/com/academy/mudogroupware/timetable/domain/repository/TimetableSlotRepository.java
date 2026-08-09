package com.academy.mudogroupware.timetable.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;

public interface TimetableSlotRepository {

    TimetableSlot save(TimetableSlot slot);

    Optional<TimetableSlot> findById(Long id);

    List<TimetableSlot> findAllByTimetableSetId(Long timetableSetId);

    List<TimetableSlot> findAllByTimetableSetIdAndClassroomCode(Long timetableSetId, String classroomCode);

    void deleteById(Long id);
}
