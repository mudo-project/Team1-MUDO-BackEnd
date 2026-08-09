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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTimetableSetsService implements GetTimetableSetsUseCase {

    private final TimetableSetRepository timetableSetRepository;
    private final Clock clock;

    @Override
    public List<TimetableSetSummaryView> getTimetableSets(Long academyId) {
        LocalDate today = LocalDate.now(clock);
        return timetableSetRepository.findAllByAcademyId(academyId).stream()
                .map(set -> toView(set, today))
                .toList();
    }

    private TimetableSetSummaryView toView(TimetableSet set, LocalDate today) {
        return new TimetableSetSummaryView(
                set.getId(), set.getName(), set.getStartDate(), set.getEndDate(), set.deriveStatus(today));
    }
}
