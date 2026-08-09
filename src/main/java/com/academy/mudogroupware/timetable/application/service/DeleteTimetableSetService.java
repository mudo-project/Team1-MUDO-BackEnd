package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTimetableSetService implements DeleteTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;

    @Override
    public void deleteTimetableSet(DeleteTimetableSetCommand command) {
        TimetableSet set = timetableSetRepository.findById(command.timetableSetId())
                .filter(found -> found.getAcademyId().equals(command.academyId()))
                .orElseThrow(TimetableSetNotFoundException::new);

        timetableSetRepository.deleteById(set.getId());
    }
}
