package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;

public final class Student {

    private final Long id;
    private final Long academyId;
    private final String name;
    private final Grade grade;
    private final String school;
    private final String phone;
    private final String parentPhone;
    private final String note;
    private final LocalDateTime createdAt;

    private Student(Long id, Long academyId, String name, Grade grade, String school, String phone,
                     String parentPhone, String note, LocalDateTime createdAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (grade == null) {
            throw new IllegalArgumentException("grade must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.grade = grade;
        this.school = school;
        this.phone = phone;
        this.parentPhone = parentPhone;
        this.note = note;
        this.createdAt = createdAt;
    }

    public static Student create(Long academyId, String name, Grade grade, String school, String phone,
                                  String parentPhone, String note, LocalDateTime now) {
        return new Student(null, academyId, name, grade, school, phone, parentPhone, note, now);
    }

    public static Student restore(Long id, Long academyId, String name, Grade grade, String school, String phone,
                                   String parentPhone, String note, LocalDateTime createdAt) {
        return new Student(id, academyId, name, grade, school, phone, parentPhone, note, createdAt);
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

    public String getSchool() {
        return school;
    }

    public String getPhone() {
        return phone;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
