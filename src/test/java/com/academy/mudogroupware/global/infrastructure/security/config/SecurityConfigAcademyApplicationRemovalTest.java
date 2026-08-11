package com.academy.mudogroupware.global.infrastructure.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 학원 신청/승인 기능 삭제 후 /api/academy-applications*의 permitAll·
 * PLATFORM:SUPER_ADMIN 전용 매처가 {@link SecurityConfig}에서 실제로 빠졌는지 회귀 검증한다.
 * @WebMvcTest 슬라이스는 실제 SecurityConfig를 로드하지 않으므로 전체 컨텍스트로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigAcademyApplicationRemovalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitNoLongerPermitsAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/academy-applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listNoLongerHasDedicatedSuperAdminMatcher() throws Exception {
        mockMvc.perform(get("/api/academy-applications"))
                .andExpect(status().isUnauthorized());
    }
}
