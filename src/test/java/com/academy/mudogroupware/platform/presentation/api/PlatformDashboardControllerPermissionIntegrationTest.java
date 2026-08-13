package com.academy.mudogroupware.platform.presentation.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.platform.application.service.PlatformDashboardQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @WebMvcTest 슬라이스는 실제 SecurityConfig(@EnableMethodSecurity)를 로드하지 않아 @PreAuthorize가
 * 동작하지 않는다. 이 테스트는 전체 컨텍스트로 PLATFORM:SUPER_ADMIN 권한이 없을 때 실제로 403이
 * 반환되는지 확인한다. 컨트롤러가 platform.dashboard.enabled=true 조건부 Bean이라 프로퍼티를 켠다.
 */
@SpringBootTest(properties = "platform.dashboard.enabled=true")
@AutoConfigureMockMvc
class PlatformDashboardControllerPermissionIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PlatformDashboardQueryService queryService;

  @Test
  void academiesReturns200WhenPlatformSuperAdminGranted() throws Exception {
    when(queryService.academies()).thenReturn(List.of());

    mockMvc.perform(get("/api/platform/academies").with(authentication(authenticatedUser("PLATFORM:SUPER_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  void academiesReturns403WhenMissingPlatformSuperAdminAuthority() throws Exception {
    mockMvc.perform(get("/api/platform/academies").with(authentication(authenticatedUser("ACCOUNT:MANAGE"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void operationalMetricsReturns403WhenMissingPlatformSuperAdminAuthority() throws Exception {
    mockMvc.perform(get("/api/platform/operational-metrics").with(authentication(authenticatedUser())))
        .andExpect(status().isForbidden());
  }

  @Test
  void apiCallFrequencyReturns403WhenMissingPlatformSuperAdminAuthority() throws Exception {
    mockMvc.perform(get("/api/platform/api-call-frequency").with(authentication(authenticatedUser())))
        .andExpect(status().isForbidden());
  }

  private Authentication authenticatedUser(String... authorities) {
    return new UsernamePasswordAuthenticationToken(
        "platform-admin",
        null,
        List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
  }
}
