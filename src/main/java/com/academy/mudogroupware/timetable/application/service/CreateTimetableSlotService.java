package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateTimetableSlotService implements CreateTimetableSlotUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public Long createSlot(CreateTimetableSlotCommand command) {
        TimetableSet set = timetableSetRepository.findById(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                set.getId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), command.color(), set.getStartDate(), set.getEndDate());

        boolean conflicts = timetableSlotRepository
                .findAllByTimetableSetIdAndClassroomCode(set.getId(), command.classroomCode()).stream()
                .anyMatch(candidate::overlaps);
        if (conflicts) {
            throw new ClassroomTimeConflictException();
        }

        return timetableSlotRepository.save(candidate).getId();
    }
}
