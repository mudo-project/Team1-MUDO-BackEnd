package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.command.SubmitAcademyApplicationCommand;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class SubmitAcademyApplicationServiceTest {

    private final AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("UTC"));
    private final SubmitAcademyApplicationService service =
            new SubmitAcademyApplicationService(academyApplicationRepository, userRepository, clock);

    @Test
    void submitsApplicationAndReturnsId() {
        LocalDateTime now = LocalDateTime.now(clock);
        AcademyApplication saved = AcademyApplication.restore(10L, "academy01", "테스트학원", null, "홍길동",
                "hong@example.com", "010-0000-0000", Plan.FREE, null, AcademyApplicationStatus.PENDING, null, null,
                null, now, now);
        when(academyApplicationRepository.save(any(AcademyApplication.class))).thenReturn(saved);

        Long applicationId = service.submit(new SubmitAcademyApplicationCommand(
                "academy01", "테스트학원", "홍길동", "hong@example.com", "010-0000-0000", Plan.FREE));

        assertThat(applicationId).isEqualTo(10L);
    }

    @Test
    void submitsApplicationWithPaidPlan() {
        LocalDateTime now = LocalDateTime.now(clock);
        AcademyApplication saved = AcademyApplication.restore(11L, "academy02", "다른학원", null, "김철수",
                "kim@example.com", "010-1111-2222", Plan.PAID, null, AcademyApplicationStatus.PENDING, null, null,
                null, now, now);
        when(academyApplicationRepository.save(any(AcademyApplication.class))).thenReturn(saved);

        Long applicationId = service.submit(new SubmitAcademyApplicationCommand(
                "academy02", "다른학원", "김철수", "kim@example.com", "010-1111-2222", Plan.PAID));

        assertThat(applicationId).isEqualTo(11L);
    }

    @Test
    void throwsWhenRequestedLoginIdAlreadyBelongsToAccount() {
        when(userRepository.existsByUsername("academy03")).thenReturn(true);

        assertThatThrownBy(() -> service.submit(new SubmitAcademyApplicationCommand(
                "academy03", "테스트학원3", "이대표", "lee@example.com", "010-2222-3333", Plan.FREE)))
                .isInstanceOf(UsernameDuplicateException.class);

        verify(academyApplicationRepository, times(0)).save(any());
    }

    @Test
    void throwsWhenRequestedLoginIdMatchesPendingApplication() {
        when(academyApplicationRepository.existsActiveRequestedLoginId("academy04")).thenReturn(true);

        assertThatThrownBy(() -> service.submit(new SubmitAcademyApplicationCommand(
                "academy04", "테스트학원4", "박대표", "park@example.com", "010-3333-4444", Plan.FREE)))
                .isInstanceOf(UsernameDuplicateException.class);

        verify(academyApplicationRepository, times(0)).save(any());
    }
}
