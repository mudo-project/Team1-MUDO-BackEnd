package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSlotService implements GetTimetableSlotUseCase {

    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public TimetableSlotView getSlot(Long timetableSetId, Long timetableSlotId) {
        TimetableSlot slot = timetableSlotRepository.findById(timetableSlotId)
                .filter(found -> found.getTimetableSetId().equals(timetableSetId))
                .orElseThrow(TimetableSlotNotFoundException::new);

        return new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName());
    }
}
