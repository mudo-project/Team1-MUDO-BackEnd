package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;

public final class Term {

    private final Long id;
    private final String name;
    private final LocalDateTime createdAt;

    private Term(Long id, String name, LocalDateTime createdAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static Term create(String name, LocalDateTime now) {
        return new Term(null, name, now);
    }

    public static Term restore(Long id, String name, LocalDateTime createdAt) {
        return new Term(id, name, createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
