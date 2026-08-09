package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableSlotJpaRepository extends JpaRepository<TimetableSlotEntity, Long> {

    List<TimetableSlotEntity> findAllByTimetableSetId(Long timetableSetId);

    List<TimetableSlotEntity> findAllByTimetableSetIdAndClassroomCode(Long timetableSetId, String classroomCode);
}
