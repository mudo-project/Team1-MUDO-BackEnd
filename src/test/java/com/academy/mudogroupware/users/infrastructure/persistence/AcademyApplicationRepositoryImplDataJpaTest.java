package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.users.domain.exception.AcademyApplicationAlreadyReviewedException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(AcademyApplicationRepositoryImpl.class)
class AcademyApplicationRepositoryImplDataJpaTest {

    @Autowired
    private AcademyApplicationRepositoryImpl academyApplicationRepository;

    @Test
    void savesAndAssignsId() {
        AcademyApplication application = AcademyApplication.submit(
                "academy01", "테스트학원", "홍길동", "hong@example.com", "010-0000-0000", Plan.FREE,
                LocalDateTime.now());

        AcademyApplication saved = academyApplicationRepository.save(application);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(AcademyApplicationStatus.PENDING);
        assertThat(saved.getPlan()).isEqualTo(Plan.FREE);
        assertThat(saved.getBusinessNo()).isNull();
    }

    @Test
    void existsActiveRequestedLoginIdReturnsTrueForPendingApplication() {
        AcademyApplication application = AcademyApplication.submit(
                "academy02", "테스트학원2", "김철수", "kim@example.com", "010-1111-2222", Plan.FREE,
                LocalDateTime.now());
        academyApplicationRepository.save(application);

        assertThat(academyApplicationRepository.existsActiveRequestedLoginId("academy02")).isTrue();
        assertThat(academyApplicationRepository.existsActiveRequestedLoginId("no-such-id")).isFalse();
    }

    @Test
    void existsActiveRequestedLoginIdReturnsTrueForApprovedApplication() {
        AcademyApplication application = AcademyApplication.submit(
                "academy03", "테스트학원3", "이대표", "lee@example.com", "010-2222-3333", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);

        academyApplicationRepository.markApproved(saved.getId(), 1L, LocalDateTime.now());

        assertThat(academyApplicationRepository.existsActiveRequestedLoginId("academy03")).isTrue();
    }

    @Test
    void existsActiveRequestedLoginIdReturnsFalseForRejectedApplication() {
        AcademyApplication application = AcademyApplication.submit(
                "academy04", "테스트학원4", "박대표", "park@example.com", "010-3333-4444", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);

        academyApplicationRepository.markRejected(saved.getId(), 1L, LocalDateTime.now(), "서류 미비");

        assertThat(academyApplicationRepository.existsActiveRequestedLoginId("academy04")).isFalse();
    }

    @Test
    void markApprovedSucceedsForPendingApplication() {
        AcademyApplication application = AcademyApplication.submit(
                "academy05", "테스트학원5", "정대표", "jung@example.com", "010-4444-5555", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);

        academyApplicationRepository.markApproved(saved.getId(), 1L, LocalDateTime.now());

        AcademyApplication found = academyApplicationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AcademyApplicationStatus.APPROVED);
    }

    @Test
    void markApprovedThrowsWhenApplicationAlreadyReviewed() {
        AcademyApplication application = AcademyApplication.submit(
                "academy06", "테스트학원6", "이영희", "yi@example.com", "010-5555-6666", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);
        academyApplicationRepository.markApproved(saved.getId(), 1L, LocalDateTime.now());

        assertThatThrownBy(() -> academyApplicationRepository.markApproved(saved.getId(), 2L, LocalDateTime.now()))
                .isInstanceOf(AcademyApplicationAlreadyReviewedException.class);
    }
}
