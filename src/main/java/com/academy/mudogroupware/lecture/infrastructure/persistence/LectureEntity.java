package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
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
public class LectureEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Grade grade;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", length = 20)
    private FeeType feeType;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureScheduleEntity> schedules = new ArrayList<>();

    @Builder
    private LectureEntity(Long academyId, String name, Grade grade, Long termId, Long subjectId, Long teacherId,
                           Long classroomId, FeeType feeType, Integer feeAmount) {
        this.academyId = academyId;
        this.name = name;
        this.grade = grade;
        this.termId = termId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.classroomId = classroomId;
        this.feeType = feeType;
        this.feeAmount = feeAmount;
    }

    public void addSchedule(LectureScheduleEntity schedule) {
        schedules.add(schedule);
        schedule.assignLecture(this);
    }
}
