package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableSetEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_set_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "operating_start_time", nullable = false)
    private LocalTime operatingStartTime;

    @Column(name = "operating_end_time", nullable = false)
    private LocalTime operatingEndTime;

    @Column(name = "operating_days", nullable = false, length = 50)
    private String operatingDays;

    @Column(name = "slot_unit_minutes", nullable = false)
    private int slotUnitMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "timetable_set_classroom", joinColumns = @JoinColumn(name = "timetable_set_id"))
    private List<TimetableClassroomEmbeddable> classrooms = new ArrayList<>();

    @Builder
    private TimetableSetEntity(Long id, Long academyId, String name, LocalDate startDate, LocalDate endDate,
                                LocalTime operatingStartTime, LocalTime operatingEndTime, String operatingDays,
                                int slotUnitMinutes, List<TimetableClassroomEmbeddable> classrooms) {
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.operatingStartTime = operatingStartTime;
        this.operatingEndTime = operatingEndTime;
        this.operatingDays = operatingDays;
        this.slotUnitMinutes = slotUnitMinutes;
        this.classrooms = classrooms != null ? new ArrayList<>(classrooms) : new ArrayList<>();
    }

    public void update(String name, LocalDate startDate, LocalDate endDate, LocalTime operatingStartTime,
                        LocalTime operatingEndTime, String operatingDays, int slotUnitMinutes,
                        List<TimetableClassroomEmbeddable> classrooms) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.operatingStartTime = operatingStartTime;
        this.operatingEndTime = operatingEndTime;
        this.operatingDays = operatingDays;
        this.slotUnitMinutes = slotUnitMinutes;
        this.classrooms.clear();
        this.classrooms.addAll(classrooms);
    }
}
