package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.domain.model.UserStatus;

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

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "must_change_pw", nullable = false)
    private boolean mustChangePw;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_scope", length = 20)
    private AdminScope adminScope;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserEntity(Long id, Long academyId, String username, String password, String name, Long roleId,
                        String phone, String email, UserStatus status, boolean mustChangePw, AccountType accountType,
                        AdminScope adminScope, LocalDateTime joinedAt, LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.roleId = roleId;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.mustChangePw = mustChangePw;
        this.accountType = accountType;
        this.adminScope = adminScope;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void changeRole(Long roleId) {
        this.roleId = roleId;
    }
}
