package com.academy.mudogroupware.calendar.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendar_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEventEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "event_start_at", nullable = false)
    private LocalDateTime eventStartAt;

    @Column(name = "event_end_at")
    private LocalDateTime eventEndAt;

    @Column(name = "is_all_day", nullable = false)
    private boolean allDay;

    @Column(length = 20)
    private String color;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder
    private CalendarEventEntity(Long id, String title, String content,
                                 LocalDateTime eventStartAt, LocalDateTime eventEndAt, boolean allDay,
                                 String color, Long createdBy) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.allDay = allDay;
        this.color = color;
        this.createdBy = createdBy;
    }

    public void update(String title, String content, LocalDateTime eventStartAt, LocalDateTime eventEndAt,
                        boolean allDay, String color) {
        this.title = title;
        this.content = content;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.allDay = allDay;
        this.color = color;
    }
}
