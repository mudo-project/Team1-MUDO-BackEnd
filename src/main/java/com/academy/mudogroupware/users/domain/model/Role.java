package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;

public final class Role {

    private final Long id;
    private final Long academyId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    private Role(Long id, Long academyId, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static Role create(Long academyId, String name, String description, LocalDateTime createdAt) {
        return new Role(null, academyId, name, description, createdAt);
    }

    public static Role restore(Long id, Long academyId, String name, String description, LocalDateTime createdAt) {
        return new Role(id, academyId, name, description, createdAt);
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

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
