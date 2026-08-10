package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;
import com.academy.mudogroupware.users.application.service.support.AccountIssuer;
import com.academy.mudogroupware.users.application.service.support.IssuedAccount;
import com.academy.mudogroupware.users.domain.event.AcademyApplicationApprovedEvent;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationAlreadyReviewedException;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.Academy;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.AcademyRepository;

class ApproveAcademyApplicationServiceTest {

    private final AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
    private final AcademyRepository academyRepository = mock(AcademyRepository.class);
    private final AccountIssuer accountIssuer = mock(AccountIssuer.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneId.of("UTC"));
    private final ApproveAcademyApplicationService service = new ApproveAcademyApplicationService(
            academyApplicationRepository, academyRepository, accountIssuer, eventPublisher, clock);

    private AcademyApplication pendingApplication() {
        return AcademyApplication.restore(1L, "newacademy01", "새학원", "111-11-11111", "홍길동",
                "hong@example.com", "010-1111-2222", null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void throwsWhenApplicationNotFound() {
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(new ApproveAcademyApplicationCommand(1L, 99L)))
                .isInstanceOf(AcademyApplicationNotFoundException.class);
    }

    @Test
    void throwsWhenAlreadyReviewed() {
        AcademyApplication reviewed = AcademyApplication.restore(1L, "newacademy01", "새학원", "111-11-11111",
                "홍길동", "hong@example.com", "010-1111-2222", null, AcademyApplicationStatus.APPROVED, null, 5L,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.of(reviewed));

        assertThatThrownBy(() -> service.approve(new ApproveAcademyApplicationCommand(1L, 99L)))
                .isInstanceOf(AcademyApplicationAlreadyReviewedException.class);
    }

    @Test
    void approvesAndCreatesAcademyAndUser() {
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication()));
        when(academyRepository.save(any())).thenAnswer(invocation -> {
            Academy input = invocation.getArgument(0);
            return Academy.restore(100L, input.getName(), input.getBusinessNo(), null, input.getApplicationId(),
                    input.getStatus(), input.getCreatedAt());
        });
        User issuedUser = User.restore(200L, 100L, "newacademy01", "hashed-password", "홍길동", "010-1111-2222",
                "hong@example.com", null, UserStatus.ACTIVE, true, AccountType.ADMIN, AdminScope.ACADEMY,
                LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDateTime.now(clock));
        when(accountIssuer.issue(eq(100L), eq("newacademy01"), eq("홍길동"), eq("010-1111-2222"),
                eq("hong@example.com"), isNull(), eq(AccountType.ADMIN), eq(AdminScope.ACADEMY),
                eq(LocalDateTime.now(clock))))
                .thenReturn(new IssuedAccount(issuedUser, "temp-password"));

        ApproveAcademyApplicationResult result = service.approve(new ApproveAcademyApplicationCommand(1L, 99L));

        assertThat(result.academyId()).isEqualTo(100L);
        assertThat(result.userId()).isEqualTo(200L);
        assertThat(result.temporaryPassword()).isEqualTo("temp-password");
        verify(academyApplicationRepository, times(1)).markApproved(1L, 99L, LocalDateTime.now(clock));
        verify(academyRepository, times(1)).assignUser(100L, 200L, LocalDateTime.now(clock));
        verify(eventPublisher, times(1)).publishEvent(new AcademyApplicationApprovedEvent(100L, 200L, 1L));
    }
}
