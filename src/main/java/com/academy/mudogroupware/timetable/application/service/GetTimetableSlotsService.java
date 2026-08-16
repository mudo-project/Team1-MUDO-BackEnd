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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSlotsService implements GetTimetableSlotsUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public List<TimetableSlotView> getSlots(Long timetableSetId) {
        log.info("event=timetable_slot_list_시작 timetableSetId={}", timetableSetId);

        timetableSetRepository.findById(timetableSetId)
                .orElseThrow(TimetableSetNotFoundException::new);

        List<TimetableSlotView> views = timetableSlotRepository.findAllByTimetableSetId(timetableSetId).stream()
                .map(this::toView)
                .toList();

        log.info("event=timetable_slot_list_완료 timetableSetId={}, count={}", timetableSetId, views.size());
        return views;
    }

    private TimetableSlotView toView(TimetableSlot slot) {
        return new TimetableSlotView(
                slot.getId(), slot.getClassType(), slot.getDayOfWeek(), slot.getClassroomCode(),
                slot.getStartTime(), slot.getEndTime(), slot.getGrade(), slot.getTeacherName(),
                slot.getSubjectName(), slot.getColor());
    }
}
