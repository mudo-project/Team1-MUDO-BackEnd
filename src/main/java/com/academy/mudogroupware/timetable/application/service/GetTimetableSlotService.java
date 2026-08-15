package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSlotService implements GetTimetableSlotUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public TimetableSlotView getSlot(Long timetableSetId, Long timetableSlotId) {
        log.info("event=timetable_slot_get_시작 timetableSetId={}, slotId={}", timetableSetId, timetableSlotId);

        timetableSetRepository.findById(timetableSetId)
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot slot = timetableSlotRepository.findById(timetableSlotId)
                .filter(found -> found.getTimetableSetId().equals(timetableSetId))
                .orElseThrow(TimetableSlotNotFoundException::new);

        TimetableSlotView view = new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName(), slot.getColor());

        log.info("event=timetable_slot_get_완료 timetableSetId={}, slotId={}", timetableSetId, timetableSlotId);
        return view;
    }
}
