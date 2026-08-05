package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;

public final class Subject {

    private final Long id;
    private final Long academyId;
    private final String name;
    private final LocalDateTime createdAt;

    private Subject(Long id, Long academyId, String name, LocalDateTime createdAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static Subject create(Long academyId, String name, LocalDateTime now) {
        return new Subject(null, academyId, name, now);
    }

    public static Subject restore(Long id, Long academyId, String name, LocalDateTime createdAt) {
        return new Subject(id, academyId, name, createdAt);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
