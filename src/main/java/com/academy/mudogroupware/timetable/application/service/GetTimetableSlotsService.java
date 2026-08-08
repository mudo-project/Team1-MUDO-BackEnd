package com.academy.mudogroupware.timetable.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSlotsService implements GetTimetableSlotsUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public List<TimetableSlotView> getSlots(Long academyId, Long timetableSetId) {
        timetableSetRepository.findById(timetableSetId)
                .filter(found -> found.getAcademyId().equals(academyId))
                .orElseThrow(TimetableSetNotFoundException::new);

        return timetableSlotRepository.findAllByTimetableSetId(timetableSetId).stream()
                .map(this::toView)
                .toList();
    }

    private TimetableSlotView toView(TimetableSlot slot) {
        return new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName());
    }
}
