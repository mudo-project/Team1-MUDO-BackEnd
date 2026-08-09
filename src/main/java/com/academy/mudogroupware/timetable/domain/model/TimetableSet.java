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
import com.academy.mudogroupware.timetable.domain.exception.TimetableNameRequiredException;

public final class TimetableSet {

    private final Long id;
    private final Long academyId;
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

    private TimetableSet(Long id, Long academyId, String name, LocalDate startDate, LocalDate endDate,
                          LocalTime operatingStartTime, LocalTime operatingEndTime, Set<DayOfWeek> operatingDays,
                          int slotUnitMinutes, List<TimetableClassroom> classrooms, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        validateAndAssign(name, startDate, endDate, operatingStartTime, operatingEndTime, operatingDays,
                slotUnitMinutes, classrooms);
        this.id = id;
        this.academyId = academyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TimetableSet create(Long academyId, String name, LocalDate startDate, LocalDate endDate,
                                       LocalTime operatingStartTime, LocalTime operatingEndTime,
                                       Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                                       List<TimetableClassroom> classrooms) {
        return new TimetableSet(null, academyId, name, startDate, endDate, operatingStartTime, operatingEndTime,
                operatingDays, slotUnitMinutes, classrooms, null, null);
    }

    public static TimetableSet restore(Long id, Long academyId, String name, LocalDate startDate, LocalDate endDate,
                                        LocalTime operatingStartTime, LocalTime operatingEndTime,
                                        Set<DayOfWeek> operatingDays, int slotUnitMinutes,
                                        List<TimetableClassroom> classrooms, LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new TimetableSet(id, academyId, name, startDate, endDate, operatingStartTime, operatingEndTime,
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
            throw new IllegalArgumentException("startDate/endDate must not be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidTimetablePeriodException();
        }
        if (operatingStartTime == null || operatingEndTime == null) {
            throw new IllegalArgumentException("operatingStartTime/operatingEndTime must not be null");
        }
        if (operatingDays == null || operatingDays.isEmpty()) {
            throw new IllegalArgumentException("operatingDays must not be empty");
        }
        if (classrooms == null || classrooms.isEmpty()) {
            throw new IllegalArgumentException("classrooms must not be empty");
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

    public Long getAcademyId() {
        return academyId;
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
