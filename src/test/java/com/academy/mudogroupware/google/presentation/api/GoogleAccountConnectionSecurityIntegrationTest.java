package com.academy.mudogroupware.google.presentation.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.presentation.security.AuthUser;

/**
 * @WebMvcTest 슬라이스는 실제 SecurityConfig(@EnableMethodSecurity 포함)를 로드하지 않아
 * /callback의 permitAll과 @PreAuthorize(ACADEMY:OWNER)를 검증할 수 없다.
 * 이 테스트는 전체 컨텍스트(실제 SecurityConfig 포함)로 그 설정이 실제로 동작하는지 확인한다.
 * (인가 실패는 컨트롤러 메서드 진입 전에 차단되므로 실제 DB 접근 없이 검증 가능하다.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class GoogleAccountConnectionSecurityIntegrationTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void callbackIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/google/connections/callback").param("error", "access_denied"))
                .andExpect(status().isFound());
    }

    @Test
    void disconnectRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/google/connections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startConnectionReturns403WhenNotAcademyOwner() throws Exception {
        mockMvc.perform(post("/api/google/connections/authorize-url")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConnectionReturns403WhenNotAcademyOwner() throws Exception {
        mockMvc.perform(get("/api/google/connections")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConnectionStatusReturns403WhenNotAcademyOwner() throws Exception {
        mockMvc.perform(get("/api/google/connections/status")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkConnectionReturns403WhenNotAcademyOwner() throws Exception {
        mockMvc.perform(post("/api/google/connections/check")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void disconnectReturns403WhenNotAcademyOwner() throws Exception {
        mockMvc.perform(delete("/api/google/connections")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
