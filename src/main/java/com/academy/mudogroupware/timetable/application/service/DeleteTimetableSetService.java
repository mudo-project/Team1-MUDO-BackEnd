package com.academy.mudogroupware.timetable.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTimetableSetService implements DeleteTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;

    @Override
    public void deleteTimetableSet(DeleteTimetableSetCommand command) {
        log.info("event=timetable_set_delete_시작 timetableSetId={}", command.timetableSetId());

        timetableSetRepository.findById(command.timetableSetId())
                .orElseThrow(TimetableSetNotFoundException::new);

        timetableSetRepository.deleteById(command.timetableSetId());
        log.info("event=timetable_set_delete_완료 timetableSetId={}", command.timetableSetId());
    }
}
