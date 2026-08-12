package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;

public final class User {

    private final Long id;
    private final String username;
    private final String password;
    private final String name;
    private final String phone;
    private final String email;
    private final Long roleId;
    private final UserStatus status;
    private final boolean mustChangePw;
    private final AccountType accountType;
    private final AdminScope adminScope;
    private final LocalDateTime joinedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private User(Long id, String username, String password, String name, String phone,
                  String email, Long roleId, UserStatus status, boolean mustChangePw, AccountType accountType,
                  AdminScope adminScope, LocalDateTime joinedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.roleId = roleId;
        this.status = status;
        this.mustChangePw = mustChangePw;
        this.accountType = accountType;
        this.adminScope = adminScope;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User restore(Long id, String username, String password, String name, String phone,
                                String email, Long roleId, UserStatus status, boolean mustChangePw,
                                AccountType accountType, AdminScope adminScope, LocalDateTime joinedAt,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, username, password, name, phone, email, roleId, status, mustChangePw,
                accountType, adminScope, joinedAt, createdAt, updatedAt);
    }

    public static User create(String username, String password, String name, Long roleId,
                               AccountType accountType, AdminScope adminScope, LocalDateTime joinedAt) {
        return new User(null, username, password, name, null, null, roleId, UserStatus.ACTIVE, true,
                accountType, adminScope, joinedAt, joinedAt, joinedAt);
    }

    public void ensureLoginAllowed() {
        if (status != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.LOGIN_RESTRICTED);
        }
    }

    public Long getId() {
        return id;
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

    public Long getRoleId() {
        return roleId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isMustChangePw() {
        return mustChangePw;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AdminScope getAdminScope() {
        return adminScope;
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
