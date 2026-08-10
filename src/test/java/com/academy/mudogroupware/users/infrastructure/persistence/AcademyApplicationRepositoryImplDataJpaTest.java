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
    void markApprovedSucceedsForPendingApplication() {
        AcademyApplication application = AcademyApplication.submit(
                "academy02", "테스트학원2", "김철수", "kim@example.com", "010-1111-2222", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);

        academyApplicationRepository.markApproved(saved.getId(), 1L, LocalDateTime.now());

        AcademyApplication found = academyApplicationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AcademyApplicationStatus.APPROVED);
    }

    @Test
    void markApprovedThrowsWhenApplicationAlreadyReviewed() {
        AcademyApplication application = AcademyApplication.submit(
                "academy03", "테스트학원3", "이영희", "lee@example.com", "010-2222-3333", Plan.FREE,
                LocalDateTime.now());
        AcademyApplication saved = academyApplicationRepository.save(application);
        academyApplicationRepository.markApproved(saved.getId(), 1L, LocalDateTime.now());

        assertThatThrownBy(() -> academyApplicationRepository.markApproved(saved.getId(), 2L, LocalDateTime.now()))
                .isInstanceOf(AcademyApplicationAlreadyReviewedException.class);
    }
}
