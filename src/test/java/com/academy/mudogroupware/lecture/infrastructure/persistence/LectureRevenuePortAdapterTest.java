package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

class LectureRevenuePortAdapterTest {

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final LectureRevenuePortAdapter adapter = new LectureRevenuePortAdapter(lectureRepository);

    @Test
    void mapsLecturesToRevenueInfo() {
        Lecture lecture = Lecture.restore(1L, "중등 수학 심화반", ClassType.CLASS, "A101", null, null, null,
                null, null, "김강사", null, FeeType.PER_MONTH, 300000,
                List.of(LectureSchedule.restore(null, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))),
                LocalDateTime.now());
        when(lectureRepository.findAll()).thenReturn(List.of(lecture));

        List<LectureRevenueInfo> result = adapter.findAll();

        assertThat(result).containsExactly(new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000));
    }
}
