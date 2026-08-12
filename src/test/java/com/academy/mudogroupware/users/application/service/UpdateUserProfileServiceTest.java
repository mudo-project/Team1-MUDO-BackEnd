package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class UpdateUserProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UpdateUserProfileService service = new UpdateUserProfileService(userRepository, roleRepository);

    private User user(long id, AccountType accountType) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, "user" + id, "hash", "기존이름", "010-0000-0000",
                "old@example.com", 5L, UserStatus.ACTIVE, false, accountType, null,
                LocalDateTime.of(2023, 1, 1, 0, 0), now, now);
    }

    @Test
    void updateMyProfileReplacesOnlyProvidedFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, AccountType.MEMBER)));

        service.updateMyProfile(1L, "010-9999-0000", null);

        verify(userRepository).updateProfile(
                eq(1L), eq("기존이름"), eq("010-9999-0000"), eq("old@example.com"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void updateMyProfileKeepsExistingValuesWhenNothingProvided() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, AccountType.MEMBER)));

        service.updateMyProfile(1L, null, null);

        verify(userRepository).updateProfile(
                eq(1L), eq("기존이름"), eq("010-0000-0000"), eq("old@example.com"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void updateMemberProfileReplacesOnlyProvidedFieldsForMember() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, AccountType.MEMBER)));
        LocalDateTime newJoinedAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        service.updateMemberProfile(2L, "새이름", null, null, newJoinedAt, null);

        verify(userRepository).updateProfile(
                eq(2L), eq("새이름"), eq("010-0000-0000"), eq("old@example.com"), eq(newJoinedAt));
        verify(userRepository, never()).changeRole(anyLong(), anyLong());
    }

    @Test
    void updateMemberProfileThrowsUserNotFoundWhenTargetIsNotMember() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, AccountType.ADMIN)));

        assertThatThrownBy(() -> service.updateMemberProfile(2L, "새이름", null, null, null, null))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateMemberProfileThrowsUserNotFoundWhenTargetDoesNotExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMemberProfile(2L, "새이름", null, null, null, null))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateMyProfileThrowsUserNotFoundWhenTargetDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMyProfile(1L, "010-9999-0000", null))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateMemberProfileChangesRoleWhenRoleIdProvidedAndValid() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, AccountType.MEMBER)));
        Role role = Role.restore(9L, "조교", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(9L)).thenReturn(Optional.of(role));

        service.updateMemberProfile(2L, null, null, null, null, 9L);

        verify(userRepository).changeRole(2L, 9L);
        verify(userRepository).updateProfile(
                eq(2L), eq("기존이름"), eq("010-0000-0000"), eq("old@example.com"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void updateMemberProfileThrowsRoleNotFoundWhenRoleIdInvalid() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, AccountType.MEMBER)));
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMemberProfile(2L, null, null, null, null, 999L))
                .isInstanceOf(RoleNotFoundException.class);

        verify(userRepository, never()).changeRole(anyLong(), anyLong());
        verify(userRepository, never()).updateProfile(anyLong(), anyString(), any(), any(), any());
    }
}
