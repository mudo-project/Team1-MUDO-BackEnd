package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationAlreadyReviewedException;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

class RejectAcademyApplicationServiceTest {

    private final AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneId.of("UTC"));
    private final RejectAcademyApplicationService service =
            new RejectAcademyApplicationService(academyApplicationRepository, clock);

    private AcademyApplication pendingApplication() {
        return AcademyApplication.restore(1L, "newacademy01", "새학원", "111-11-11111", "홍길동",
                "hong@example.com", "010-1111-2222", null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void throwsWhenApplicationNotFound() {
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(new RejectAcademyApplicationCommand(1L, 99L, "사유")))
                .isInstanceOf(AcademyApplicationNotFoundException.class);
    }

    @Test
    void throwsWhenAlreadyReviewed() {
        AcademyApplication reviewed = AcademyApplication.restore(1L, "newacademy01", "새학원", "111-11-11111",
                "홍길동", "hong@example.com", "010-1111-2222", null, AcademyApplicationStatus.REJECTED, "기존사유",
                5L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.of(reviewed));

        assertThatThrownBy(() -> service.reject(new RejectAcademyApplicationCommand(1L, 99L, "사유")))
                .isInstanceOf(AcademyApplicationAlreadyReviewedException.class);
    }

    @Test
    void rejectsPendingApplication() {
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication()));

        service.reject(new RejectAcademyApplicationCommand(1L, 99L, "사업자번호 확인 불가"));

        verify(academyApplicationRepository, times(1))
                .markRejected(1L, 99L, LocalDateTime.now(clock), "사업자번호 확인 불가");
    }
}
