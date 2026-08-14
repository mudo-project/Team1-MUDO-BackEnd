package com.academy.mudogroupware.calendar.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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
}
