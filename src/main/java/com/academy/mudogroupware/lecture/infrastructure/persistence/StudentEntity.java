package com.academy.mudogroupware.lecture.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import com.academy.mudogroupware.lecture.domain.model.Grade;

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
@Table(name = "student")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Grade grade;

    @Column(length = 100)
    private String school;

    @Column(length = 20)
    private String phone;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(length = 500)
    private String note;

    @Builder
    private StudentEntity(Long academyId, String name, Grade grade, String school, String phone,
                           String parentPhone, String note) {
        this.academyId = academyId;
        this.name = name;
        this.grade = grade;
        this.school = school;
        this.phone = phone;
        this.parentPhone = parentPhone;
        this.note = note;
    }
}
