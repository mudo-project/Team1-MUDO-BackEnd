package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSlotCommand;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.UnsupportedSlotScopeException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTimetableSlotService implements DeleteTimetableSlotUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    @Override
    public void deleteSlot(DeleteTimetableSlotCommand command) {
        log.info("event=timetable_slot_delete_시작 timetableSetId={}, slotId={}",
                command.timetableSetId(), command.timetableSlotId());

        if (command.scope() != UpdateScope.ALL) {
            throw new UnsupportedSlotScopeException();
        }

        timetableSetRepository.findById(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        TimetableSlot slot = timetableSlotRepository.findById(command.timetableSlotId())
                .filter(found -> found.getTimetableSetId().equals(command.timetableSetId()))
                .orElseThrow(TimetableSlotNotFoundException::new);

        timetableSlotRepository.deleteById(slot.getId());
        log.info("event=timetable_slot_delete_완료 timetableSetId={}, slotId={}",
                command.timetableSetId(), command.timetableSlotId());
    }
}
