package com.academy.mudogroupware.calendar.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.calendar.domain.exception.CalendarTitleRequiredException;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;

public final class CalendarEvent {

    private final Long id;
    private final Long academyId;
    private String title;
    private String content;
    private LocalDateTime eventStartAt;
    private LocalDateTime eventEndAt;
    private boolean allDay;
    private String color;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CalendarEvent(Long id, Long academyId, String title, String content,
                           LocalDateTime eventStartAt, LocalDateTime eventEndAt, boolean allDay,
                           String color, Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new CalendarTitleRequiredException();
        }
        if (eventStartAt == null) {
            throw new IllegalArgumentException("eventStartAt must not be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }
        if (eventEndAt != null && eventEndAt.isBefore(eventStartAt)) {
            throw new InvalidCalendarPeriodException();
        }
        this.id = id;
        this.academyId = academyId;
        this.title = title;
        this.content = content;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.allDay = allDay;
        this.color = color;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CalendarEvent create(Long academyId, String title, String content,
                                        LocalDateTime eventStartAt, LocalDateTime eventEndAt,
                                        boolean allDay, String color, Long createdBy) {
        return new CalendarEvent(null, academyId, title, content, eventStartAt, eventEndAt,
                allDay, color, createdBy, null, null);
    }

    public static CalendarEvent restore(Long id, Long academyId, String title, String content,
                                         LocalDateTime eventStartAt, LocalDateTime eventEndAt, boolean allDay,
                                         String color, Long createdBy, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new CalendarEvent(id, academyId, title, content, eventStartAt, eventEndAt,
                allDay, color, createdBy, createdAt, updatedAt);
    }

    public void update(String title, String content, LocalDateTime eventStartAt, LocalDateTime eventEndAt,
                        boolean allDay, String color) {
        if (title == null || title.isBlank()) {
            throw new CalendarTitleRequiredException();
        }
        if (eventStartAt == null) {
            throw new IllegalArgumentException("eventStartAt must not be null");
        }
        if (eventEndAt != null && eventEndAt.isBefore(eventStartAt)) {
            throw new InvalidCalendarPeriodException();
        }
        this.title = title;
        this.content = content;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.allDay = allDay;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getEventStartAt() {
        return eventStartAt;
    }

    public LocalDateTime getEventEndAt() {
        return eventEndAt;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public String getColor() {
        return color;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
