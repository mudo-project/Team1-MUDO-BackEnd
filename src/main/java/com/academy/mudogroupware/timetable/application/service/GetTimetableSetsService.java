package com.academy.mudogroupware.timetable.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.timetable.application.query.TimetableSetSummaryView;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetsUseCase;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSetsService implements GetTimetableSetsUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final Clock clock;

    @Override
    public List<TimetableSetSummaryView> getTimetableSets() {
        log.info("event=timetable_set_list_시작");

        LocalDate today = LocalDate.now(clock);
        List<TimetableSetSummaryView> views = timetableSetRepository.findAll().stream()
                .map(set -> toView(set, today))
                .toList();

        log.info("event=timetable_set_list_완료 count={}", views.size());
        return views;
    }

    private TimetableSetSummaryView toView(TimetableSet set, LocalDate today) {
        return new TimetableSetSummaryView(
                set.getId(), set.getName(), set.getStartDate(), set.getEndDate(), set.deriveStatus(today));
    }
}
