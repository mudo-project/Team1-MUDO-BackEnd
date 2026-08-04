package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;

public final class User {

    private final Long id;
    private final Long academyId;
    private final String username;
    private final String password;
    private final String name;
    private final String phone;
    private final String email;
    private final String role;
    private final UserStatus status;
    private final boolean mustChangePw;
    private final boolean platformAdmin;
    private final LocalDateTime joinedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private User(Long id, Long academyId, String username, String password, String name, String phone,
                  String email, String role, UserStatus status, boolean mustChangePw, boolean platformAdmin,
                  LocalDateTime joinedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.status = status;
        this.mustChangePw = mustChangePw;
        this.platformAdmin = platformAdmin;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User restore(Long id, Long academyId, String username, String password, String name, String phone,
                                String email, String role, UserStatus status, boolean mustChangePw,
                                boolean platformAdmin, LocalDateTime joinedAt, LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        return new User(id, academyId, username, password, name, phone, email, role, status, mustChangePw,
                platformAdmin, joinedAt, createdAt, updatedAt);
    }

    public void ensureLoginAllowed() {
        if (status != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.LOGIN_RESTRICTED);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isMustChangePw() {
        return mustChangePw;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
