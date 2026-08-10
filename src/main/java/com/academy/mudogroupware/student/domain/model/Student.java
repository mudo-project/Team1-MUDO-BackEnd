package com.academy.mudogroupware.student.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;

public final class Student {

    private final Long id;
    private String name;
    private StudentGrade grade;
    private String school;
    private String phone;
    private String parentPhone;
    private String note;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Student(Long id, String name, StudentGrade grade, String school, String phone,
                    String parentPhone, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
        validateRequired(name, grade, createdAt, updatedAt);
        this.id = id;
        this.name = name.trim();
        this.grade = grade;
        this.school = normalize(school);
        this.phone = normalize(phone);
        this.parentPhone = normalize(parentPhone);
        this.note = normalize(note);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Student create(String name, StudentGrade grade, String school, String phone,
                                 String parentPhone, String note, LocalDateTime now) {
        return new Student(null, name, grade, school, phone, parentPhone, note, now, now);
    }

    public static Student restore(Long id, String name, StudentGrade grade, String school,
                                  String phone, String parentPhone, String note, LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        return new Student(id, name, grade, school, phone, parentPhone, note, createdAt, updatedAt);
    }

    public void update(String name, StudentGrade grade, String school, String phone, String parentPhone, String note,
                       LocalDateTime updatedAt) {
        validateRequired(name, grade, createdAt, updatedAt);
        this.name = name.trim();
        this.grade = grade;
        this.school = normalize(school);
        this.phone = normalize(phone);
        this.parentPhone = normalize(parentPhone);
        this.note = normalize(note);
        this.updatedAt = updatedAt;
    }

    private static void validateRequired(String name, StudentGrade grade, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        if (name == null || name.isBlank()) {
            throw new StudentException(StudentErrorCode.STUDENT_NAME_REQUIRED);
        }
        if (grade == null) {
            throw new StudentException(StudentErrorCode.STUDENT_GRADE_REQUIRED);
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StudentGrade getGrade() {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
