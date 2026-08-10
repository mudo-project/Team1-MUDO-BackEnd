package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, TimetableSlotPersistenceAdapter.class})
class TimetableSlotPersistenceAdapterDataJpaTest {

    @Autowired
    private TimetableSlotPersistenceAdapter adapter;

    @Test
    void savesAndFindsSlot() {
        TimetableSlot slot = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));

        TimetableSlot saved = adapter.save(slot);
        Optional<TimetableSlot> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getClassroomCode()).isEqualTo("601");
        assertThat(found.get().getGrade()).isEqualTo(Grade.HIGH_3);
    }

    @Test
    void findsAllByTimetableSetIdAndClassroomCode() {
        TimetableSlot slot601 = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        TimetableSlot slot602 = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "602", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        adapter.save(slot601);
        adapter.save(slot602);

        List<TimetableSlot> found = adapter.findAllByTimetableSetIdAndClassroomCode(1L, "601");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getClassroomCode()).isEqualTo("601");
    }

    @Test
    void deletesSlotById() {
        TimetableSlot slot = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16));
        TimetableSlot saved = adapter.save(slot);

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }
}
