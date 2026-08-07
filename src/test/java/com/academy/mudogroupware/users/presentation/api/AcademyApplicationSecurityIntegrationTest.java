package com.academy.mudogroupware.users.presentation.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.users.application.usecase.GetAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.ListAcademyApplicationsUseCase;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;

/**
 * @WebMvcTest 슬라이스는 실제 SecurityConfig를 로드하지 않아 PLATFORM:SUPER_ADMIN 기반
 * 필터체인 인가를 검증할 수 없다. 이 테스트는 전체 컨텍스트(실제 SecurityConfig 포함)로
 * 익명/일반 사용자/SUPER ADMIN 각각에 대해 실제 응답 코드가 나오는지 확인한다.
 *
 * <p>테스트 프로필은 Flyway/DDL 자동 생성을 모두 꺼둔 채(H2, 스키마 없음) 전체 컨텍스트를
 * 띄우므로, UseCase는 {@link MockitoBean}으로 대체해 실제 DB 접근 없이 필터체인 동작만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AcademyApplicationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAcademyApplicationsUseCase listAcademyApplicationsUseCase;

    @MockitoBean
    private GetAcademyApplicationUseCase getAcademyApplicationUseCase;

    @Test
    void listIsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/academy-applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listIsForbiddenForAuthenticatedNonSuperAdmin() throws Exception {
        TestingAuthenticationToken nonSuperAdmin =
                new TestingAuthenticationToken("teacher", null, "WORKSPACE:READ");

        mockMvc.perform(get("/api/academy-applications").with(authentication(nonSuperAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listIsOkForPlatformSuperAdmin() throws Exception {
        when(listAcademyApplicationsUseCase.listApplications()).thenReturn(List.of());
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken("superadmin", null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(get("/api/academy-applications").with(authentication(superAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void detailIsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/academy-applications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailIsForbiddenForAuthenticatedNonSuperAdmin() throws Exception {
        TestingAuthenticationToken nonSuperAdmin =
                new TestingAuthenticationToken("teacher", null, "WORKSPACE:READ");

        mockMvc.perform(get("/api/academy-applications/1").with(authentication(nonSuperAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailIsOkForPlatformSuperAdmin() throws Exception {
        AcademyApplication application = AcademyApplication.restore(
                1L, "academy01", "테스트학원", "123-45-67890", "홍길동", "a@a.com", "010-0000-0000",
                null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(getAcademyApplicationUseCase.getApplication(1L)).thenReturn(application);
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken("superadmin", null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(get("/api/academy-applications/1").with(authentication(superAdmin)))
                .andExpect(status().isOk());
    }
}
