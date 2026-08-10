package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.domain.model.Academy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(AcademyManagementRepositoryImpl.class)
class AcademyManagementRepositoryImplDataJpaTest {

    @Autowired
    private AcademyManagementRepositoryImpl academyRepository;

    @Autowired
    private AcademyManagementJpaRepository academyJpaRepository;

    @Test
    @Transactional
    void assignUserUpdatesUserIdAndUpdatedAtOnManagedEntity() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Academy saved = academyRepository.save(Academy.create("테스트학원", "111-11-11111", 1L, createdAt));
        assertThat(saved.getUserId()).isNull();

        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
        academyRepository.assignUser(saved.getId(), 42L, updatedAt);
        academyJpaRepository.flush();

        AcademyEntity reloaded = academyJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(42L);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
