package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.lecture.domain.exception.LectureNameRequiredException;
import com.academy.mudogroupware.lecture.domain.exception.LectureScheduleRequiredException;

public final class Lecture {

    private final Long id;
    private final String name;
    private final ClassType classType;
    private final String classroomCode;
    private final Grade grade;
    private final Long termId;
    private final Long subjectId;
    private final Long teacherId;
    private final Long classroomId;
    private final String teacherName;
    private final String subjectName;
    private final FeeType feeType;
    private final Integer feeAmount;
    private final List<LectureSchedule> schedules;
    private final LocalDateTime createdAt;

    private Lecture(Long id, String name, ClassType classType, String classroomCode, Grade grade, Long termId,
                     Long subjectId, Long teacherId, Long classroomId, String teacherName, String subjectName,
                     FeeType feeType, Integer feeAmount, List<LectureSchedule> schedules, LocalDateTime createdAt) {
        if (name == null || name.isBlank()) {
            throw new LectureNameRequiredException();
        }
        if (classType == null) {
            throw new IllegalArgumentException("classType must not be null");
        }
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new IllegalArgumentException("classroomCode must not be blank");
        }
        if (schedules == null || schedules.isEmpty()) {
            throw new LectureScheduleRequiredException();
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.name = name;
        this.classType = classType;
        this.classroomCode = classroomCode;
        this.grade = grade;
        this.termId = termId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.classroomId = classroomId;
        this.teacherName = teacherName;
        this.subjectName = subjectName;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
        this.schedules = new ArrayList<>(schedules);
        this.createdAt = createdAt;
    }

    public static Lecture create(String name, ClassType classType, String classroomCode, Grade grade, Long termId,
                                  Long subjectId, Long teacherId, Long classroomId, String teacherName,
                                  String subjectName, FeeType feeType, Integer feeAmount,
                                  List<LectureSchedule> schedules, LocalDateTime now) {
        return new Lecture(null, name, classType, classroomCode, grade, termId, subjectId, teacherId, classroomId,
                teacherName, subjectName, feeType, feeAmount, schedules, now);
    }

    public static Lecture create(String name, Grade grade, Long termId, Long subjectId,
                                  Long teacherId, Long classroomId, FeeType feeType, Integer feeAmount,
                                  List<LectureSchedule> schedules, LocalDateTime now) {
        return create(name, ClassType.CLASS, String.valueOf(classroomId), grade, termId, subjectId, teacherId,
                classroomId, null, null, feeType, feeAmount, schedules, now);
    }

    public static Lecture restore(Long id, String name, ClassType classType, String classroomCode, Grade grade,
                                   Long termId, Long subjectId, Long teacherId, Long classroomId, String teacherName,
                                   String subjectName, FeeType feeType, Integer feeAmount,
                                   List<LectureSchedule> schedules, LocalDateTime createdAt) {
        return new Lecture(id, name, classType, classroomCode, grade, termId, subjectId, teacherId, classroomId,
                teacherName, subjectName, feeType, feeAmount, schedules, createdAt);
    }

    public static Lecture restore(Long id, String name, Grade grade, Long termId, Long subjectId,
                                   Long teacherId, Long classroomId, FeeType feeType, Integer feeAmount,
                                   List<LectureSchedule> schedules, LocalDateTime createdAt) {
        return restore(id, name, ClassType.CLASS, String.valueOf(classroomId), grade, termId, subjectId, teacherId,
                classroomId, null, null, feeType, feeAmount, schedules, createdAt);
    }

    public boolean conflictsWith(LectureSchedule candidate) {
        return schedules.stream().anyMatch(schedule -> schedule.overlaps(candidate));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ClassType getClassType() {
        return classType;
    }

    public String getClassroomCode() {
        return classroomCode;
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

    public String getTeacherName() {
        return teacherName;
    }

    public String getSubjectName() {
        return subjectName;
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
