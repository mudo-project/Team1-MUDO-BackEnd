package com.academy.mudogroupware.users.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;
import com.academy.mudogroupware.users.application.usecase.ApproveAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.GetAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.ListAcademyApplicationsUseCase;
import com.academy.mudogroupware.users.application.usecase.RejectAcademyApplicationUseCase;
import com.academy.mudogroupware.users.domain.exception.AcademyApplicationNotFoundException;
import com.academy.mudogroupware.users.domain.model.AcademyApplication;
import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;

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

    private static final AuthUser SUPER_ADMIN_PRINCIPAL =
            new AuthUser(99L, "superadmin", null, null, "SUPER_ADMIN", AccountType.ADMIN, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAcademyApplicationsUseCase listAcademyApplicationsUseCase;

    @MockitoBean
    private GetAcademyApplicationUseCase getAcademyApplicationUseCase;

    @MockitoBean
    private ApproveAcademyApplicationUseCase approveAcademyApplicationUseCase;

    @MockitoBean
    private RejectAcademyApplicationUseCase rejectAcademyApplicationUseCase;

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
                Plan.FREE, null, AcademyApplicationStatus.PENDING, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(getAcademyApplicationUseCase.getApplication(1L)).thenReturn(application);
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken("superadmin", null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(get("/api/academy-applications/1").with(authentication(superAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACADEMY_APPLICATION_200_2"))
                .andExpect(jsonPath("$.data.applicationId").value(1));

        verify(getAcademyApplicationUseCase).getApplication(1L);
    }

    @Test
    void detailReturns404WhenApplicationNotFound() throws Exception {
        when(getAcademyApplicationUseCase.getApplication(99L))
                .thenThrow(new AcademyApplicationNotFoundException());
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken("superadmin", null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(get("/api/academy-applications/99").with(authentication(superAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_404_3"));
    }

    @Test
    void approveIsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/academy-applications/1/approve").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveIsForbiddenForAuthenticatedNonSuperAdmin() throws Exception {
        TestingAuthenticationToken nonSuperAdmin =
                new TestingAuthenticationToken("teacher", null, "WORKSPACE:READ");

        mockMvc.perform(post("/api/academy-applications/1/approve")
                        .with(authentication(nonSuperAdmin))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveIsOkForPlatformSuperAdmin() throws Exception {
        when(approveAcademyApplicationUseCase.approve(any()))
                .thenReturn(new ApproveAcademyApplicationResult(10L, 20L, "TempPass123!"));
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken(SUPER_ADMIN_PRINCIPAL, null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(post("/api/academy-applications/1/approve")
                        .with(authentication(superAdmin))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACADEMY_APPLICATION_200_3"))
                .andExpect(jsonPath("$.data.academyId").value(10))
                .andExpect(jsonPath("$.data.userId").value(20))
                .andExpect(jsonPath("$.data.temporaryPassword").value("TempPass123!"));

        verify(approveAcademyApplicationUseCase).approve(new ApproveAcademyApplicationCommand(1L, 99L));
    }

    @Test
    void rejectIsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/academy-applications/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"사유\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectIsForbiddenForAuthenticatedNonSuperAdmin() throws Exception {
        TestingAuthenticationToken nonSuperAdmin =
                new TestingAuthenticationToken("teacher", null, "WORKSPACE:READ");

        mockMvc.perform(post("/api/academy-applications/1/reject")
                        .with(authentication(nonSuperAdmin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"사유\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectIsNoContentForPlatformSuperAdmin() throws Exception {
        TestingAuthenticationToken superAdmin =
                new TestingAuthenticationToken(SUPER_ADMIN_PRINCIPAL, null, "PLATFORM:SUPER_ADMIN");

        mockMvc.perform(post("/api/academy-applications/1/reject")
                        .with(authentication(superAdmin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"사업자번호 확인 불가\"}"))
                .andExpect(status().isNoContent());

        verify(rejectAcademyApplicationUseCase)
                .reject(new RejectAcademyApplicationCommand(1L, 99L, "사업자번호 확인 불가"));
    }
}
