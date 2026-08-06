package com.academy.mudogroupware.lecture.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.lecture.domain.exception.InvalidLectureScheduleTimeException;

public final class LectureSchedule {

    private final Long id;
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private LectureSchedule(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("dayOfWeek must not be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime/endTime must not be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidLectureScheduleTimeException();
        }
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static LectureSchedule create(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return new LectureSchedule(null, dayOfWeek, startTime, endTime);
    }

    public static LectureSchedule restore(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return new LectureSchedule(id, dayOfWeek, startTime, endTime);
    }

    public boolean overlaps(LectureSchedule other) {
        if (this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    public Long getId() {
        return id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
