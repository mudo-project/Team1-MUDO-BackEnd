package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.lecture.domain.exception.LectureNameRequiredException;
import com.academy.mudogroupware.lecture.domain.exception.LectureScheduleRequiredException;

public final class Lecture {

    private final Long id;
    private final Long academyId;
    private final String name;
    private final Grade grade;
    private final Long termId;
    private final Long subjectId;
    private final Long teacherId;
    private final Long classroomId;
    private final FeeType feeType;
    private final Integer feeAmount;
    private final List<LectureSchedule> schedules;
    private final LocalDateTime createdAt;

    private Lecture(Long id, Long academyId, String name, Grade grade, Long termId, Long subjectId, Long teacherId,
                     Long classroomId, FeeType feeType, Integer feeAmount, List<LectureSchedule> schedules,
                     LocalDateTime createdAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new LectureNameRequiredException();
        }
        if (grade == null) {
            throw new IllegalArgumentException("grade must not be null");
        }
        if (termId == null) {
            throw new IllegalArgumentException("termId must not be null");
        }
        if (subjectId == null) {
            throw new IllegalArgumentException("subjectId must not be null");
        }
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId must not be null");
        }
        if (classroomId == null) {
            throw new IllegalArgumentException("classroomId must not be null");
        }
        if (schedules == null || schedules.isEmpty()) {
            throw new LectureScheduleRequiredException();
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.grade = grade;
        this.termId = termId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.classroomId = classroomId;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
        this.schedules = new ArrayList<>(schedules);
        this.createdAt = createdAt;
    }

    public static Lecture create(Long academyId, String name, Grade grade, Long termId, Long subjectId,
                                  Long teacherId, Long classroomId, FeeType feeType, Integer feeAmount,
                                  List<LectureSchedule> schedules, LocalDateTime now) {
        return new Lecture(null, academyId, name, grade, termId, subjectId, teacherId, classroomId, feeType,
                feeAmount, schedules, now);
    }

    public static Lecture restore(Long id, Long academyId, String name, Grade grade, Long termId, Long subjectId,
                                   Long teacherId, Long classroomId, FeeType feeType, Integer feeAmount,
                                   List<LectureSchedule> schedules, LocalDateTime createdAt) {
        return new Lecture(id, academyId, name, grade, termId, subjectId, teacherId, classroomId, feeType,
                feeAmount, schedules, createdAt);
    }

    public boolean conflictsWith(LectureSchedule candidate) {
        return schedules.stream().anyMatch(schedule -> schedule.overlaps(candidate));
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

    public Grade getGrade() {
        return grade;
    }

    public Long getTermId() {
        return termId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public Integer getFeeAmount() {
        return feeAmount;
    }

    public List<LectureSchedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
