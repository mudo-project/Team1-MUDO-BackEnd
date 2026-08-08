package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateTimetableSetService implements CreateTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;

    @Override
    public Long createTimetableSet(CreateTimetableSetCommand command) {
        TimetableSet timetableSet = TimetableSet.create(
                command.academyId(), command.name(), command.startDate(), command.endDate(),
                command.operatingStartTime(), command.operatingEndTime(), command.operatingDays(),
                command.slotUnitMinutes(), command.classrooms());

        return timetableSetRepository.save(timetableSet).getId();
    }
}
