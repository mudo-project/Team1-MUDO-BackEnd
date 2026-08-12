package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

public final class Role {

    private final Long id;
    private final String name;
    private final String description;
    private final String color;
    private final LocalDateTime createdAt;
    private final Set<String> permissionCodes;

    private Role(Long id, String name, String description, String color, LocalDateTime createdAt,
                 Set<String> permissionCodes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.createdAt = createdAt;
        this.permissionCodes = Set.copyOf(permissionCodes);
    }

    public static Role create(String name, String description, LocalDateTime createdAt) {
        return create(name, description, null, createdAt);
    }

    public static Role create(String name, String description, String color,
                               LocalDateTime createdAt) {
        return new Role(null, name, description, color, createdAt, Set.of());
    }

    public static Role restore(Long id, String name, String description, LocalDateTime createdAt,
                                Set<String> permissionCodes) {
        return restore(id, name, description, null, createdAt, permissionCodes);
    }

    public static Role restore(Long id, String name, String description, String color,
                                LocalDateTime createdAt, Set<String> permissionCodes) {
        return new Role(id, name, description, color, createdAt, permissionCodes);
    }

    public Role withPermissionCodes(Set<String> permissionCodes) {
        return new Role(id, name, description, color, createdAt, permissionCodes);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }
}
