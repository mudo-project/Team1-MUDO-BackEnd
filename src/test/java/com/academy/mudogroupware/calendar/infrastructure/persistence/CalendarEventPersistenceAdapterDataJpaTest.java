package com.academy.mudogroupware.calendar.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, CalendarEventPersistenceAdapter.class})
class CalendarEventPersistenceAdapterDataJpaTest {

    @Autowired
    private CalendarEventPersistenceAdapter adapter;

    private CalendarEvent newEvent() {
        return CalendarEvent.create(
                "동시성 테스트 일정", "내용", LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0), false, "FFCC00", 1L);
    }

    @Test
    void savesAndFindsEvent() {
        CalendarEvent saved = adapter.save(newEvent());

        Optional<CalendarEvent> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("동시성 테스트 일정");
    }

    @Test
    void updatesEventFields() {
        CalendarEvent saved = adapter.save(newEvent());
        CalendarEvent loaded = adapter.findById(saved.getId()).orElseThrow();

        loaded.update("수정된 제목", "수정된 내용", loaded.getEventStartAt(), loaded.getEventEndAt(),
                loaded.isAllDay(), loaded.getColor());
        adapter.save(loaded);

        CalendarEvent reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    void findAllByPeriodIncludesEventThatStartedBeforeButIsStillOngoingDuringPeriod() {
        CalendarEvent spanning = CalendarEvent.create(
                "워크숍", "3일간 진행",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 3, 18, 0),
                false, "blue", 1L);
        adapter.save(spanning);

        List<CalendarEvent> events = adapter.findAllByPeriod(
                LocalDateTime.of(2026, 8, 2, 0, 0),
                LocalDateTime.of(2026, 8, 2, 23, 59, 59));

        assertThat(events).extracting(CalendarEvent::getTitle).containsExactly("워크숍");
    }

    @Test
    void findAllByPeriodExcludesEventThatEndedBeforePeriodStarts() {
        CalendarEvent past = CalendarEvent.create(
                "지난 행사", "종료됨",
                LocalDateTime.of(2026, 7, 28, 9, 0),
                LocalDateTime.of(2026, 7, 30, 18, 0),
                false, "gray", 1L);
        adapter.save(past);

        List<CalendarEvent> events = adapter.findAllByPeriod(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59, 59));

        assertThat(events).isEmpty();
    }

    @Test
    void findAllByPeriodIncludesInstantEventWithoutEndTime() {
        CalendarEvent instant = CalendarEvent.create(
                "짧은 안내", null,
                LocalDateTime.of(2026, 8, 15, 9, 0),
                null, false, "red", 1L);
        adapter.save(instant);

        List<CalendarEvent> events = adapter.findAllByPeriod(
                LocalDateTime.of(2026, 8, 15, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59, 59));

        assertThat(events).extracting(CalendarEvent::getTitle).containsExactly("짧은 안내");
    }
}
