package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateTimetableSetService implements UpdateTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;

    @Override
    public void updateTimetableSet(UpdateTimetableSetCommand command) {
        log.info("event=timetable_set_update_시작 timetableSetId={}", command.timetableSetId());

        TimetableSet set = timetableSetRepository.findByIdForUpdate(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        set.update(command.name(), command.startDate(), command.endDate(), command.operatingStartTime(),
                command.operatingEndTime(), command.operatingDays(), command.slotUnitMinutes(),
                command.classrooms());

        timetableSetRepository.save(set);
        log.info("event=timetable_set_update_완료 timetableSetId={}", command.timetableSetId());
    }
}
