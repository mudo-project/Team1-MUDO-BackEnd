package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.Plan;

class AcademyApplicationRepositoryImplTest {

    @Test
    void convertsRequestedLoginIdUniqueConstraintViolationToUsernameDuplicateException() {
        AcademyApplicationJpaRepository jpaRepository = mock(AcademyApplicationJpaRepository.class);
        AcademyApplicationRepositoryImpl adapter = new AcademyApplicationRepositoryImpl(jpaRepository);
        AcademyApplication application = AcademyApplication.submit(
                "academy01", "테스트학원", "홍길동", "hong@example.com", "010-0000-0000", Plan.FREE,
                LocalDateTime.now());
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Duplicate entry 'academy01' for key 'academy_application.uk_academy_application_requested_login_id_active'");
        when(jpaRepository.saveAndFlush(any(AcademyApplicationEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(application))
                .isInstanceOf(UsernameDuplicateException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolation() {
        AcademyApplicationJpaRepository jpaRepository = mock(AcademyApplicationJpaRepository.class);
        AcademyApplicationRepositoryImpl adapter = new AcademyApplicationRepositoryImpl(jpaRepository);
        AcademyApplication application = AcademyApplication.submit(
                "academy02", "테스트학원2", "김철수", "kim@example.com", "010-1111-2222", Plan.FREE,
                LocalDateTime.now());
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some unrelated constraint");
        when(jpaRepository.saveAndFlush(any(AcademyApplicationEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(application)).isSameAs(violation);
    }
}
