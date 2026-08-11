package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class UpdateUserProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UpdateUserProfileService service = new UpdateUserProfileService(userRepository);

    private User user(long id, long academyId, AccountType accountType) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, academyId, "user" + id, "hash", "기존이름", "010-0000-0000",
                "old@example.com", 5L, UserStatus.ACTIVE, false, accountType, null,
                LocalDateTime.of(2023, 1, 1, 0, 0), now, now);
    }

    @Test
    void updateMyProfileReplacesOnlyProvidedFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, 1L, AccountType.MEMBER)));

        service.updateMyProfile(1L, "010-9999-0000", null);

        verify(userRepository).updateProfile(
                eq(1L), eq("기존이름"), eq("010-9999-0000"), eq("old@example.com"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void updateMyProfileKeepsExistingValuesWhenNothingProvided() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, 1L, AccountType.MEMBER)));

        service.updateMyProfile(1L, null, null);

        verify(userRepository).updateProfile(
                eq(1L), eq("기존이름"), eq("010-0000-0000"), eq("old@example.com"),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)));
    }

    @Test
    void updateMemberProfileReplacesOnlyProvidedFieldsForMemberInSameAcademy() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, AccountType.MEMBER)));
        LocalDateTime newJoinedAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        service.updateMemberProfile(1L, 2L, "새이름", null, null, newJoinedAt);

        verify(userRepository).updateProfile(
                eq(2L), eq("새이름"), eq("010-0000-0000"), eq("old@example.com"), eq(newJoinedAt));
    }

    @Test
    void updateMemberProfileThrowsWhenTargetIsNotMember() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1L, AccountType.ADMIN)));

        assertThatThrownBy(() -> service.updateMemberProfile(1L, 2L, "새이름", null, null, null))
                .isInstanceOf(UserException.class);
    }

    @Test
    void updateMemberProfileThrowsWhenTargetIsInDifferentAcademy() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 99L, AccountType.MEMBER)));

        assertThatThrownBy(() -> service.updateMemberProfile(1L, 2L, "새이름", null, null, null))
                .isInstanceOf(UserException.class);
    }
}
