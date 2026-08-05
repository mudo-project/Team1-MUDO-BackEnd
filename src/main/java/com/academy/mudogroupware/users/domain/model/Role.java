package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

public final class Role {

    private final Long id;
    private final Long academyId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;
    private final Set<String> permissionCodes;

    private Role(Long id, Long academyId, String name, String description, LocalDateTime createdAt,
                 Set<String> permissionCodes) {
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.permissionCodes = permissionCodes;
    }

    public static Role create(Long academyId, String name, String description, LocalDateTime createdAt) {
        return new Role(null, academyId, name, description, createdAt, Set.of());
    }

    public static Role restore(Long id, Long academyId, String name, String description, LocalDateTime createdAt,
                                Set<String> permissionCodes) {
        return new Role(id, academyId, name, description, createdAt, permissionCodes);
    }

    public Role withPermissionCodes(Set<String> permissionCodes) {
        return new Role(id, academyId, name, description, createdAt, permissionCodes);
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

    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }
}
