package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, TimetableSetPersistenceAdapter.class})
class TimetableSetPersistenceAdapterDataJpaTest {

    @Autowired
    private TimetableSetPersistenceAdapter adapter;

    @Test
    void savesAndFindsTimetableSetWithClassrooms() {
        TimetableSet set = TimetableSet.create(
                "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY),
                30, List.of(new TimetableClassroom("6층", "601")));

        TimetableSet saved = adapter.save(set);
        Optional<TimetableSet> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("2026 여름특강");
        assertThat(found.get().getClassrooms()).containsExactly(new TimetableClassroom("6층", "601"));
    }

    @Test
    void deletesTimetableSetById() {
        TimetableSet set = TimetableSet.create(
                "삭제될 세트", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY),
                30, List.of(new TimetableClassroom("6층", "601")));
        TimetableSet saved = adapter.save(set);

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isEmpty();
    }
}
