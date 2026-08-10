package com.academy.mudogroupware.timetable.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSlotException;

public final class TimetableSlot {

    private final Long id;
    private final Long timetableSetId;
    private ClassType classType;
    private DayOfWeek dayOfWeek;
    private String classroomCode;
    private LocalTime startTime;
    private LocalTime endTime;
    private Grade grade;
    private String teacherName;
    private String subjectName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TimetableSlot(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                           String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                           String teacherName, String subjectName, LocalDate effectiveFrom,
                           LocalDate effectiveUntil, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (timetableSetId == null) {
            throw new InvalidTimetableSlotException("timetableSetId");
        }
        if (classType == null) {
            throw new InvalidTimetableSlotException("classType");
        }
        if (dayOfWeek == null) {
            throw new InvalidTimetableSlotException("dayOfWeek");
        }
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new InvalidTimetableSlotException("classroomCode");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidTimetableSlotException("timeRange");
        }
        if (grade == null) {
            throw new InvalidTimetableSlotException("grade");
        }
        if (effectiveFrom == null || effectiveUntil == null || effectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidTimetableSlotException("effectivePeriod");
        }
        this.id = id;
        this.timetableSetId = timetableSetId;
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TimetableSlot create(Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                        String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                        String teacherName, String subjectName, LocalDate effectiveFrom,
                                        LocalDate effectiveUntil) {
        return new TimetableSlot(null, timetableSetId, classType, dayOfWeek, classroomCode, startTime, endTime,
                grade, teacherName, subjectName, effectiveFrom, effectiveUntil, null, null);
    }

    public static TimetableSlot restore(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                         String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                         String teacherName, String subjectName, LocalDate effectiveFrom,
                                         LocalDate effectiveUntil, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new TimetableSlot(id, timetableSetId, classType, dayOfWeek, classroomCode, startTime, endTime, grade,
                teacherName, subjectName, effectiveFrom, effectiveUntil, createdAt, updatedAt);
    }

    public void applyFullUpdate(ClassType classType, DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime,
                                 LocalTime endTime, Grade grade, String teacherName, String subjectName) {
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new InvalidTimetableSlotException("classroomCode");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidTimetableSlotException("timeRange");
        }
        if (grade == null) {
            throw new InvalidTimetableSlotException("grade");
        }
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
    }

    public void closeEffectiveUntil(LocalDate newEffectiveUntil) {
        if (newEffectiveUntil == null || newEffectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidTimetableSlotException("effectiveUntil");
        }
        this.effectiveUntil = newEffectiveUntil;
    }

    public boolean overlaps(TimetableSlot other) {
        if (!this.classroomCode.equals(other.classroomCode) || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        boolean timeOverlaps = this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
        boolean rangeOverlaps = !this.effectiveFrom.isAfter(other.effectiveUntil)
                && !other.effectiveFrom.isAfter(this.effectiveUntil);
        return timeOverlaps && rangeOverlaps;
    }

    public Long getId() {
        return id;
    }

    public Long getTimetableSetId() {
        return timetableSetId;
    }

    public ClassType getClassType() {
        return classType;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public String getClassroomCode() {
        return classroomCode;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Grade getGrade() {
        return grade;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
