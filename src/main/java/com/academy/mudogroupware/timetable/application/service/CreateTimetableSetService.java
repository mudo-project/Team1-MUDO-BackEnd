package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateTimetableSetService implements CreateTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;

    @Override
    public Long createTimetableSet(CreateTimetableSetCommand command) {
        log.info("event=timetable_set_create_시작 name={}", command.name());

        TimetableSet timetableSet = TimetableSet.create(
                command.name(), command.startDate(), command.endDate(),
                command.operatingStartTime(), command.operatingEndTime(), command.operatingDays(),
                command.slotUnitMinutes(), command.classrooms());

        Long timetableSetId = timetableSetRepository.save(timetableSet).getId();
        log.info("event=timetable_set_create_완료 name={}, timetableSetId={}", command.name(), timetableSetId);
        return timetableSetId;
    }
}
