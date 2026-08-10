package com.academy.mudogroupware.timetable.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.timetable.domain.exception.DuplicateClassroomCodeException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetablePeriodException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSetException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableNameRequiredException;

public final class TimetableSet {

    private final Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime operatingStartTime;
    private LocalTime operatingEndTime;
    private Set<DayOfWeek> operatingDays;
    private int slotUnitMinutes;
    private List<TimetableClassroom> classrooms;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TimetableSet(Long id, String name, LocalDate startDate, LocalDate endDate,
                          LocalTime operatingStartTime, LocalTime operatingEndTime, Set<DayOfWeek> operatingDays,
                          int slotUnitMinutes, List<TimetableClassroom> classrooms, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        validateAndAssign(name, startDate, endDate, operatingStartTime, operatingEndTime, operatingDays,
                slotUnitMinutes, classrooms);
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TimetableSet create(String name, LocalDate startDate, LocalDate endDate,
                                       LocalTime operatingStartTime, LocalTime operatingEndTime,
                                       Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                                       List<TimetableClassroom> classrooms) {
        return new TimetableSet(null, name, startDate, endDate, operatingStartTime, operatingEndTime,
                operatingDays, slotUnitMinutes, classrooms, null, null);
    }

    public static TimetableSet restore(Long id, String name, LocalDate startDate, LocalDate endDate,
                                        LocalTime operatingStartTime, LocalTime operatingEndTime,
                                        Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                                        List<TimetableClassroom> classrooms, LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new TimetableSet(id, name, startDate, endDate, operatingStartTime, operatingEndTime,
                operatingDays, slotUnitMinutes, classrooms, createdAt, updatedAt);
    }

    public void update(String name, LocalDate startDate, LocalDate endDate, LocalTime operatingStartTime,
                        LocalTime operatingEndTime, Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                        List<TimetableClassroom> classrooms) {
        validateAndAssign(name, startDate, endDate, operatingStartTime, operatingEndTime, operatingDays,
                slotUnitMinutes, classrooms);
    }

    private void validateAndAssign(String name, LocalDate startDate, LocalDate endDate,
                                    LocalTime operatingStartTime, LocalTime operatingEndTime,
                                    Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                                    List<TimetableClassroom> classrooms) {
        if (name == null || name.isBlank()) {
            throw new TimetableNameRequiredException();
        }
        if (startDate == null || endDate == null) {
            throw new InvalidTimetableSetException("period");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidTimetablePeriodException();
        }
        if (operatingStartTime == null || operatingEndTime == null) {
            throw new InvalidTimetableSetException("operatingTime");
        }
        if (operatingDays == null || operatingDays.isEmpty()) {
            throw new InvalidTimetableSetException("operatingDays");
        }
        if (classrooms == null || classrooms.isEmpty()) {
            throw new InvalidTimetableSetException("classrooms");
        }
        Set<String> codes = new HashSet<>();
        for (TimetableClassroom classroom : classrooms) {
            if (!codes.add(classroom.code())) {
                throw new DuplicateClassroomCodeException();
            }
        }
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.operatingStartTime = operatingStartTime;
        this.operatingEndTime = operatingEndTime;
        this.operatingDays = new HashSet<>(operatingDays);
        this.slotUnitMinutes = slotUnitMinutes;
        this.classrooms = new ArrayList<>(classrooms);
    }

    public TimetableSetStatus deriveStatus(LocalDate today) {
        if (today.isBefore(startDate)) {
            return TimetableSetStatus.PLANNED;
        }
        if (today.isAfter(endDate)) {
            return TimetableSetStatus.ENDED;
        }
        return TimetableSetStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalTime getOperatingStartTime() {
        return operatingStartTime;
    }

    public LocalTime getOperatingEndTime() {
        return operatingEndTime;
    }

    public Set<DayOfWeek> getOperatingDays() {
        return Collections.unmodifiableSet(operatingDays);
    }

    public int getSlotUnitMinutes() {
        return slotUnitMinutes;
    }

    public List<TimetableClassroom> getClassrooms() {
        return Collections.unmodifiableList(classrooms);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
