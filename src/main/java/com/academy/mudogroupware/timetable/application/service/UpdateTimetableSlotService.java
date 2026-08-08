package com.academy.mudogroupware.timetable.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.UnsupportedSlotScopeException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateTimetableSlotService implements UpdateTimetableSlotUseCase {

    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public void updateSlot(UpdateTimetableSlotCommand command) {
        if (command.scope() != UpdateScope.ALL) {
            throw new UnsupportedSlotScopeException();
        }

        TimetableSlot slot = timetableSlotRepository.findById(command.timetableSlotId())
                .filter(found -> found.getTimetableSetId().equals(command.timetableSetId()))
                .orElseThrow(TimetableSlotNotFoundException::new);

        TimetableSlot candidate = TimetableSlot.create(
                slot.getTimetableSetId(), command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName(), slot.getEffectiveFrom(), slot.getEffectiveUntil());

        List<TimetableSlot> othersInClassroom = timetableSlotRepository
                .findAllByTimetableSetIdAndClassroomCode(slot.getTimetableSetId(), command.classroomCode()).stream()
                .filter(other -> !other.getId().equals(slot.getId()))
                .toList();
        boolean conflicts = othersInClassroom.stream().anyMatch(candidate::overlaps);
        if (conflicts) {
            throw new ClassroomTimeConflictException();
        }

        slot.applyFullUpdate(command.classType(), command.dayOfWeek(), command.classroomCode(),
                command.startTime(), command.endTime(), command.grade(), command.teacherName(),
                command.subjectName());

        timetableSlotRepository.save(slot);
    }
}
