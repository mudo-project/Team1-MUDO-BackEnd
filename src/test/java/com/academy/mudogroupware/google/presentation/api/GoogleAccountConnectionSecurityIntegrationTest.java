package com.academy.mudogroupware.google.presentation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @WebMvcTest 슬라이스는 실제 SecurityConfig를 로드하지 않아 /callback의 permitAll을 검증할 수 없다.
 * 이 테스트는 전체 컨텍스트(실제 SecurityConfig 포함)로 그 설정이 실제로 동작하는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GoogleAccountConnectionSecurityIntegrationTest {

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
}
