package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, LectureRepositoryImpl.class})
class LectureRepositoryImplDataJpaTest {

    @Autowired
    private LectureRepositoryImpl repository;

    @Test
    void savesAndRestoresTimetableAlignedFields() {
        Lecture lecture = Lecture.create(
                "고1 수학 정규반",
                ClassType.CLASS,
                "601",
                null,
                null,
                null,
                null,
                null,
                "김선생",
                "수학",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0));

        Lecture saved = repository.save(lecture);
        Lecture found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getClassType()).isEqualTo(ClassType.CLASS);
        assertThat(found.getClassroomCode()).isEqualTo("601");
        assertThat(found.getTeacherName()).isEqualTo("김선생");
        assertThat(found.getSubjectName()).isEqualTo("수학");
        assertThat(found.getTeacherId()).isNull();
    }

    @Test
    void detectsOverlapByClassroomCode() {
        Lecture lecture = Lecture.create(
                "고1 수학 정규반",
                ClassType.CLASS,
                "601",
                null,
                null,
                null,
                null,
                null,
                "김선생",
                "수학",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0));
        repository.save(lecture);

        assertThat(repository.existsOverlap("601", DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)))
                .isTrue();
        assertThat(repository.existsOverlap("602", DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)))
                .isFalse();
    }
}
