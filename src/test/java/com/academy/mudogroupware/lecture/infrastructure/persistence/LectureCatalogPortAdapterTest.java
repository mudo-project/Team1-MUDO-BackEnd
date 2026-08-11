package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.application.port.TeacherDirectoryPort;
import com.academy.mudogroupware.lecture.application.port.TeacherInfo;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureRepository;
import com.academy.mudogroupware.student.application.port.LectureCatalogInfo;

class LectureCatalogPortAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 10, 0);

    private final LectureRepository lectureRepository = mock(LectureRepository.class);
    private final TeacherDirectoryPort teacherDirectoryPort = mock(TeacherDirectoryPort.class);
    private final LectureCatalogPortAdapter adapter =
            new LectureCatalogPortAdapter(lectureRepository, teacherDirectoryPort);

    @Test
    void includesTeacherNameFromUsersDirectory() {
        Lecture lecture = Lecture.restore(100L, "Math", Grade.HIGH_1, 10L, 20L, 30L, 40L,
                FeeType.PER_MONTH, 300000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                NOW);
        when(lectureRepository.findAllById(List.of(100L))).thenReturn(List.of(lecture));
        when(teacherDirectoryPort.findTeachers(List.of(30L)))
                .thenReturn(Map.of(30L, new TeacherInfo(30L, "Teacher Park", 1L, "ACTIVE")));

        Map<Long, LectureCatalogInfo> result = adapter.findByIds(List.of(100L));

        assertThat(result.get(100L).teacherName()).isEqualTo("Teacher Park");
    }

    @Test
    void usesStoredTeacherNameBeforeUsersDirectoryFallback() {
        Lecture lecture = Lecture.restore(100L, "Math", ClassType.CLASS, "601", Grade.HIGH_1,
                null, null, null, null, "Stored Teacher", "Math", FeeType.PER_MONTH, 300000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                NOW);
        when(lectureRepository.findAllById(List.of(100L))).thenReturn(List.of(lecture));

        Map<Long, LectureCatalogInfo> result = adapter.findByIds(List.of(100L));

        assertThat(result.get(100L).teacherName()).isEqualTo("Stored Teacher");
        verify(teacherDirectoryPort, never()).findTeachers(any());
    }
}
