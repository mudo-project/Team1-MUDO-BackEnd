package com.academy.mudogroupware.timetable.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableSlotEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_slot_id")
    private Long id;

    @Column(name = "timetable_set_id", nullable = false)
    private Long timetableSetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false, length = 20)
    private ClassType classType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "classroom_code", nullable = false, length = 50)
    private String classroomCode;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Grade grade;

    @Column(name = "teacher_name", length = 50)
    private String teacherName;

    @Column(name = "subject_name", length = 50)
    private String subjectName;

    @Column(length = 6, nullable = false)
    private String color;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until", nullable = false)
    private LocalDate effectiveUntil;

    @Builder
    private TimetableSlotEntity(Long id, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek,
                                 String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
                                 String teacherName, String subjectName, String color, LocalDate effectiveFrom,
                                 LocalDate effectiveUntil) {
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
        this.color = color;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
    }

    public void update(ClassType classType, DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime,
                        LocalTime endTime, Grade grade, String teacherName, String subjectName, String color,
                        LocalDate effectiveFrom, LocalDate effectiveUntil) {
        this.classType = classType;
        this.dayOfWeek = dayOfWeek;
        this.classroomCode = classroomCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.grade = grade;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.color = color;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
    }
}
