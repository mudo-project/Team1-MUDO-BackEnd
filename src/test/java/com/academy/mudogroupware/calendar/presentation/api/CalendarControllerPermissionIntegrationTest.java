package com.academy.mudogroupware.calendar.presentation.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.presentation.security.AuthUser;

/**
 * @WebMvcTest 슬라이스는 실제 SecurityConfig(@EnableMethodSecurity)를 로드하지 않아 @PreAuthorize가
 * 동작하지 않는다. 이 테스트는 전체 컨텍스트로 CALENDAR:MANAGE 권한이 없을 때 실제로 403이 반환되는지 확인한다.
 * (인가 실패는 컨트롤러 메서드 진입 전에 차단되므로 실제 DB 접근 없이 검증 가능하다.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerPermissionIntegrationTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createEventReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "title": "제목",
                  "eventStartAt": "2026-08-03T10:00:00"
                }
                """;

        mockMvc.perform(post("/api/calendars")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEventReturns403WhenMissingManageAuthority() throws Exception {
        mockMvc.perform(delete("/api/calendars/101")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEventReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "title": "제목",
                  "eventStartAt": "2026-08-04T12:30:00",
                  "allDay": false
                }
                """;

        mockMvc.perform(patch("/api/calendars/101")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
