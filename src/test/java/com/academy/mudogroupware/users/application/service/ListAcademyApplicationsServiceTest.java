package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;
import com.academy.mudogroupware.users.domain.repository.AcademyApplicationRepository;

class ListAcademyApplicationsServiceTest {

    @Test
    void returnsAllApplicationsFromRepository() {
        AcademyApplicationRepository academyApplicationRepository = mock(AcademyApplicationRepository.class);
        AcademyApplication application = AcademyApplication.restore(
                1L, "academy01", "테스트학원", "123-45-67890", "홍길동", "a@a.com", "010-0000-0000",
                Plan.FREE, null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(academyApplicationRepository.findAll()).thenReturn(List.of(application));
        ListAcademyApplicationsService service = new ListAcademyApplicationsService(academyApplicationRepository);

        List<AcademyApplication> result = service.listApplications();

        assertThat(result).containsExactly(application);
    }
}
