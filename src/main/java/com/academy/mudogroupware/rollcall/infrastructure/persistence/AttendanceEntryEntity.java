package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

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
@Table(name = "attendance_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceEntryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(length = 500)
    private String note;

    @Builder
    private AttendanceEntryEntity(Long academyId, Long lectureId, Long studentId, LocalDate date,
                                   AttendanceStatus status, String note) {
        this.academyId = academyId;
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.date = date;
        this.status = status;
        this.note = note;
    }

    public void changeStatus(AttendanceStatus status, String note) {
        this.status = status;
        this.note = note;
    }
}
