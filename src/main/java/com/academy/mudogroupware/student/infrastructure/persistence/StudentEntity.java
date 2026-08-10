package com.academy.mudogroupware.student.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.SoftDeleteTimeEntity;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

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
import lombok.Setter;

@Entity
@Table(name = "student")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity extends SoftDeleteTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long id;

    @Setter
    @Column(nullable = false, length = 50)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentGrade grade;

    @Setter
    @Column(length = 100)
    private String school;

    @Setter
    @Column(length = 30)
    private String phone;

    @Setter
    @Column(name = "parent_phone", length = 30)
    private String parentPhone;

    @Setter
    @Column(length = 500)
    private String note;

    @Builder
    private StudentEntity(Long id, String name, StudentGrade grade, String school, String phone,
                          String parentPhone, String note) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.school = school;
        this.phone = phone;
        this.parentPhone = parentPhone;
        this.note = note;
    }
}
