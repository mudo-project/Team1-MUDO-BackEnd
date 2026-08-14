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
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.model.Lecture;
import com.academy.mudogroupware.lecture.domain.model.LectureSchedule;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;

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

    @Test
    void updatesExistingLectureAndReplacesSchedules() {
        Lecture saved = repository.save(Lecture.create(
                "Original Math",
                ClassType.CLASS,
                "601",
                Grade.HIGH_1,
                null,
                null,
                null,
                null,
                "Teacher A",
                "Math",
                FeeType.PER_SESSION,
                50000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0)));

        Lecture updated = Lecture.restore(
                saved.getId(),
                "Updated Math",
                ClassType.SPECIAL,
                "602",
                Grade.HIGH_2,
                null,
                null,
                null,
                null,
                "Teacher B",
                "Advanced Math",
                FeeType.PER_MONTH,
                300000,
                List.of(LectureSchedule.create(DayOfWeek.TUESDAY, LocalTime.of(20, 0), LocalTime.of(22, 0))),
                saved.getCreatedAt());

        repository.save(updated);

        Lecture found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Updated Math");
        assertThat(found.getClassType()).isEqualTo(ClassType.SPECIAL);
        assertThat(found.getClassroomCode()).isEqualTo("602");
        assertThat(found.getSchedules()).hasSize(1);
        assertThat(found.getSchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(repository.findAllById(List.of(saved.getId()))).hasSize(1);
    }

    @Test
    void filtersLecturesByTimetableVisibleFields() {
        Lecture target = repository.save(Lecture.create(
                "High Math",
                ClassType.CLASS,
                "601",
                Grade.HIGH_1,
                null,
                null,
                null,
                null,
                "Teacher A",
                "Math",
                FeeType.PER_MONTH,
                300000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0)));
        repository.save(Lecture.create(
                "High English",
                ClassType.CLASS,
                "602",
                Grade.HIGH_1,
                null,
                null,
                null,
                null,
                "Teacher B",
                "English",
                FeeType.PER_MONTH,
                300000,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0)));

        List<Lecture> lectures = repository.findAll(
                new LectureFilter(null, Grade.HIGH_1, "Math", "Teacher A", "601", DayOfWeek.MONDAY),
                0,
                20).content();

        assertThat(lectures).extracting(Lecture::getId).containsExactly(target.getId());
    }

    @Test
    void softDeletedLectureIsHiddenFromFindAllAndOverlapCheck() {
        Lecture saved = repository.save(Lecture.create(
                "Delete Target",
                ClassType.CLASS,
                "601",
                null,
                null,
                null,
                null,
                null,
                "Teacher A",
                "Math",
                null,
                null,
                List.of(LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))),
                LocalDateTime.of(2026, 8, 11, 9, 0)));

        repository.deleteById(saved.getId(), LocalDateTime.of(2026, 8, 13, 10, 0));

        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.findAll(new LectureFilter(null, null, null, null, null, null), 0, 20).content())
                .isEmpty();
        assertThat(repository.findAllById(List.of(saved.getId()))).isEmpty();
        assertThat(repository.existsOverlap("601", DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)))
                .isFalse();
        assertThat(repository.existsOverlapExcludingLecture(saved.getId(), "601", DayOfWeek.MONDAY,
                LocalTime.of(20, 0), LocalTime.of(22, 0))).isFalse();
    }
}
