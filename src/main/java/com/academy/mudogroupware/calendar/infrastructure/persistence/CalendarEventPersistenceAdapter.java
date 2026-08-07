package com.academy.mudogroupware.calendar.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CalendarEventPersistenceAdapter implements CalendarEventRepository {

    private final CalendarEventJpaRepository calendarEventJpaRepository;

    @Override
    public CalendarEvent save(CalendarEvent calendarEvent) {
        CalendarEventEntity entity = calendarEvent.getId() != null
                ? updateExisting(calendarEvent)
                : toEntity(calendarEvent);
        return toDomain(calendarEventJpaRepository.save(entity));
    }

    @Override
    public Optional<CalendarEvent> findById(Long id) {
        return calendarEventJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CalendarEvent> findAllByAcademyIdAndPeriod(Long academyId, LocalDateTime from, LocalDateTime to) {
        return calendarEventJpaRepository.findAllByAcademyIdAndEventStartAtBetween(academyId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        calendarEventJpaRepository.deleteById(id);
    }

    private CalendarEventEntity updateExisting(CalendarEvent domain) {
        CalendarEventEntity entity = calendarEventJpaRepository.getReferenceById(domain.getId());
        entity.update(domain.getTitle(), domain.getContent(), domain.getEventStartAt(),
                domain.getEventEndAt(), domain.isAllDay(), domain.getColor());
        return entity;
    }

    private CalendarEventEntity toEntity(CalendarEvent domain) {
        return CalendarEventEntity.builder()
                .id(domain.getId())
                .academyId(domain.getAcademyId())
                .title(domain.getTitle())
                .content(domain.getContent())
                .eventStartAt(domain.getEventStartAt())
                .eventEndAt(domain.getEventEndAt())
                .allDay(domain.isAllDay())
                .color(domain.getColor())
                .createdBy(domain.getCreatedBy())
                .build();
    }

    private CalendarEvent toDomain(CalendarEventEntity entity) {
        return CalendarEvent.restore(
                entity.getId(), entity.getAcademyId(), entity.getTitle(), entity.getContent(),
                entity.getEventStartAt(), entity.getEventEndAt(), entity.isAllDay(), entity.getColor(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
