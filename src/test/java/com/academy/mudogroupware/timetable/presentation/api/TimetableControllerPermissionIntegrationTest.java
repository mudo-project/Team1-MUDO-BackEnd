package com.academy.mudogroupware.timetable.presentation.api;

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
 * 동작하지 않는다. 이 테스트는 전체 컨텍스트로 TIMETABLE:MANAGE 권한이 없을 때 실제로 403이 반환되는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TimetableControllerPermissionIntegrationTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createTimetableSetReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "name": "이름",
                  "startDate": "2026-07-20",
                  "endDate": "2026-08-16",
                  "operatingStartTime": "08:30",
                  "operatingEndTime": "22:00",
                  "operatingDays": ["MONDAY"],
                  "slotUnitMinutes": 30,
                  "classrooms": [{"floor": "6층", "codes": ["601"]}]
                }
                """;

        mockMvc.perform(post("/api/timetables")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTimetableSetReturns403WhenMissingManageAuthority() throws Exception {
        String body = """
                {
                  "name": "이름",
                  "startDate": "2026-07-20",
                  "endDate": "2026-08-16",
                  "operatingStartTime": "08:30",
                  "operatingEndTime": "22:00",
                  "operatingDays": ["MONDAY"],
                  "slotUnitMinutes": 30,
                  "classrooms": [{"floor": "6층", "codes": ["601"]}]
                }
                """;

        mockMvc.perform(patch("/api/timetables/1")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTimetableSetReturns403WhenMissingManageAuthority() throws Exception {
        mockMvc.perform(delete("/api/timetables/1")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
