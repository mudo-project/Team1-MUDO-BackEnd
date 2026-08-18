package com.academy.mudogroupware.platform.presentation.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.platform.application.service.TenantDirectoryQueryService;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.TenantDirectoryEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 이 API는 프론트가 로그인 전에 호출해야 하므로 인증 없이 열려 있어야 한다. SecurityConfig에
 * permitAll 매처가 빠지면 anyRequest().authenticated()에 걸려 401/403이 난다.
 */
@SpringBootTest(properties = "platform.dashboard.enabled=true")
@AutoConfigureMockMvc
class TenantDirectoryControllerPermissionIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TenantDirectoryQueryService queryService;

  @Test
  void resolveReturns200WithoutAuthentication() throws Exception {
    when(queryService.resolve("academy-a"))
        .thenReturn(new TenantDirectoryEntry("academy-a", "academy-a.ieum.store"));

    mockMvc.perform(get("/api/public/tenants/academy-a")).andExpect(status().isOk());
  }

  @Test
  void resolveReturns404WithoutAuthenticationWhenCodeUnknown() throws Exception {
    when(queryService.resolve("academy-unknown"))
        .thenThrow(new PlatformException(PlatformErrorCode.ACADEMY_NOT_FOUND));

    mockMvc.perform(get("/api/public/tenants/academy-unknown")).andExpect(status().isNotFound());
  }
}
