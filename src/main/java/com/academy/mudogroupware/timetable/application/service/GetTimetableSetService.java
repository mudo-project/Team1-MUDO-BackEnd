package com.academy.mudogroupware.timetable.application.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSetService implements GetTimetableSetUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final Clock clock;

    @Override
    public TimetableSetDetailView getTimetableSet(Long timetableSetId) {
        log.info("event=timetable_set_get_시작 timetableSetId={}", timetableSetId);

        TimetableSet set = timetableSetRepository.findById(timetableSetId)
                .orElseThrow(TimetableSetNotFoundException::new);

        LocalDate today = LocalDate.now(clock);
        TimetableSetDetailView view = new TimetableSetDetailView(
                set.getId(), set.getName(), set.getStartDate(), set.getEndDate(),
                set.getOperatingStartTime(), set.getOperatingEndTime(), set.getOperatingDays(),
                set.getSlotUnitMinutes(), set.getClassrooms(), set.deriveStatus(today));

        log.info("event=timetable_set_get_완료 timetableSetId={}", timetableSetId);
        return view;
    }
}
