package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
                "academy01", "테스트학원", "홍길동", "hong@example.com", "010-0000-0000", Plan.FREE,
                LocalDateTime.now());
        academyApplicationRepository.save(application);

        assertThat(academyApplicationRepository.existsActiveRequestedLoginId("academy01")).isTrue();
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
}
