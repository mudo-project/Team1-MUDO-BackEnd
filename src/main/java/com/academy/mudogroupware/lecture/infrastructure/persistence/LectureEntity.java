package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.academy.mudogroupware.global.infrastructure.persistence.SoftDeleteTimeEntity;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lecture")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureEntity extends SoftDeleteTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", length = 20)
    private ClassType classType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Grade grade;

    @Column(name = "term_id")
    private Long termId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_name", length = 50)
    private String subjectName;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "teacher_name", length = 50)
    private String teacherName;

    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "classroom_code", length = 50)
    private String classroomCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", length = 20)
    private FeeType feeType;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<LectureScheduleEntity> schedules = new ArrayList<>();

    @Builder
    private LectureEntity(String name, ClassType classType, String classroomCode, Grade grade, Long termId,
                           Long subjectId, String subjectName, Long teacherId, String teacherName,
                           Long classroomId, FeeType feeType, Integer feeAmount) {
        this.name = name;
        this.classType = classType;
        this.classroomCode = classroomCode;
        this.grade = grade;
        this.termId = termId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.classroomId = classroomId;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
    }

    public void addSchedule(LectureScheduleEntity schedule) {
        schedules.add(schedule);
        schedule.assignLecture(this);
    }

    public void update(String name, ClassType classType, String classroomCode, Grade grade, Long termId,
                       Long subjectId, String subjectName, Long teacherId, String teacherName,
                       Long classroomId, FeeType feeType, Integer feeAmount) {
        this.name = name;
        this.classType = classType;
        this.classroomCode = classroomCode;
        this.grade = grade;
        this.termId = termId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.classroomId = classroomId;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
    }

    public void replaceSchedules(List<LectureScheduleEntity> newSchedules) {
        schedules.clear();
        newSchedules.forEach(this::addSchedule);
    }
}
