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
}
