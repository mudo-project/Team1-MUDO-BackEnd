package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

class GetAcademyApplicationServiceTest {

    @Test
    void returnsApplicationWhenFound() {
        AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
        AcademyApplication application = AcademyApplication.restore(
                1L, "academy01", "테스트학원", "123-45-67890", "홍길동", "a@a.com", "010-0000-0000",
                Plan.FREE, null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(academyApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        GetAcademyApplicationService service = new GetAcademyApplicationService(academyApplicationRepository);

        AcademyApplication result = service.getApplication(1L);

        assertThat(result).isEqualTo(application);
    }

    @Test
    void throwsWhenApplicationNotFound() {
        AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
        when(academyApplicationRepository.findById(99L)).thenReturn(Optional.empty());
        GetAcademyApplicationService service = new GetAcademyApplicationService(academyApplicationRepository);

        assertThatThrownBy(() -> service.getApplication(99L))
                .isInstanceOf(AcademyApplicationNotFoundException.class);
    }
}
